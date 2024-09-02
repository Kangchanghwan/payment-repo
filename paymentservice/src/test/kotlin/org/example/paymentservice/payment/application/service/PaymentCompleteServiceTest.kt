import io.mockk.every
import io.mockk.mockk
import org.example.paymentservice.PaymentServiceApplication
import org.example.paymentservice.payment.application.port.`in`.CheckOutCommand
import org.example.paymentservice.payment.application.port.`in`.CheckOutUseCase
import org.example.paymentservice.payment.application.port.`in`.PaymentCompleteUseCase
import org.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import org.example.paymentservice.payment.application.port.out.PaymentExecutionPort
import org.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import org.example.paymentservice.payment.application.port.out.PaymentValidationPort
import org.example.paymentservice.payment.application.service.PaymentConfirmService
import org.example.paymentservice.payment.application.service.PaymentErrorHandler
import org.example.paymentservice.payment.domain.*
import org.example.paymentservice.payment.test.PaymentDatabaseHelper
import org.example.paymentservice.payment.test.PaymentTestConfiguration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(classes = [PaymentServiceApplication::class])
@Import(PaymentTestConfiguration::class)
class PaymentCompleteServiceTest(
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper,
    @Autowired private val checkOutUseCase: CheckOutUseCase,
    @Autowired private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    @Autowired private val paymentValidationPort: PaymentValidationPort,
    @Autowired private val paymentErrorHandler: PaymentErrorHandler,
    @Autowired private val paymentCompleteUseCase: PaymentCompleteUseCase
) {
    private val mockPaymentExecutorPort = mockk<PaymentExecutionPort>()

    @BeforeEach
    fun clean() {
        paymentDatabaseHelper.clean().block()
    }

    @Test
    fun `should update payment given a WalletEventMessage`() {
        Hooks.onOperatorDebug()

        val orderId = createPaymentEventWithSuccessStatus()

        val walletEventMessage = WalletEventMessage(
            type = WalletEventMessageType.SUCCESS,
            payload = mapOf("orderId" to orderId),
        )

        paymentCompleteUseCase.completePayment(walletEventMessage).block()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertTrue(paymentEvent.isWalletUpdateDone())
        assertFalse(paymentEvent.isLedgerUpdateDone())
        assertFalse(paymentEvent.isPaymentDone())
    }

    @Test
    fun `should update payment given a LedgerEventMessage`() {
        Hooks.onOperatorDebug()

        val orderId = createPaymentEventWithSuccessStatus()

        val ledgerEventMessage = LedgerEventMessage(
            type = LedgerEventMessageType.SUCCESS,
            payload = mapOf("orderId" to orderId),
        )

        paymentCompleteUseCase.completePayment(ledgerEventMessage).block()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertTrue(paymentEvent.isLedgerUpdateDone())
        assertFalse(paymentEvent.isWalletUpdateDone())
        assertFalse(paymentEvent.isPaymentDone())
    }
    @Test
    fun `should update payment given a LedgerEventMessage and WalletEventMessage`() {
        Hooks.onOperatorDebug()

        val orderId = createPaymentEventWithSuccessStatus()

        val ledgerEventMessage = LedgerEventMessage(
            type = LedgerEventMessageType.SUCCESS,
            payload = mapOf("orderId" to orderId),
        )
        val walletEventMessage = WalletEventMessage(
            type = WalletEventMessageType.SUCCESS,
            payload = mapOf("orderId" to orderId),
        )

        paymentCompleteUseCase.completePayment(ledgerEventMessage).block()
        paymentCompleteUseCase.completePayment(walletEventMessage).block()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertTrue(paymentEvent.isLedgerUpdateDone())
        assertTrue(paymentEvent.isWalletUpdateDone())
        assertTrue(paymentEvent.isPaymentDone())
    }


    private fun createPaymentEventWithSuccessStatus(): String {
        val orderId = UUID.randomUUID().toString()

        val checkOutCommand = CheckOutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkOutResult = checkOutUseCase.checkOut(checkOutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = checkOutResult.orderId,
            amount = checkOutResult.amount
        )

        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentValidationPort = paymentValidationPort,
            paymentExecutionPort = mockPaymentExecutorPort,
            paymentErrorHandler = paymentErrorHandler
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.CARD,
                totalAmount = paymentConfirmCommand.amount,
                orderName = "test_order_name",
                pspConfirmationStatus = PSPConfirmationsStatus.DONE,
                approvedAt = LocalDateTime.now(),
                pspRawData = "{}"
            ),
            isSuccess = true,
            isUnknown = false,
            isFailure = false,
            isRetryable = false
        )

        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)
        paymentConfirmService.confirm(paymentConfirmCommand).block()!!
        return orderId;
    }
}