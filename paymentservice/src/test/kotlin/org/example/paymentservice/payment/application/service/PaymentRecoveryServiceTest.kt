package org.example.paymentservice.payment.application.service

import io.mockk.every
import io.mockk.mockk
import org.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import org.example.paymentservice.payment.application.port.`in`.CheckOutCommand
import org.example.paymentservice.payment.application.port.`in`.CheckOutUseCase
import org.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import org.example.paymentservice.payment.application.port.out.*
import org.example.paymentservice.payment.domain.*
import org.example.paymentservice.payment.test.PaymentDatabaseHelper
import org.example.paymentservice.payment.test.PaymentTestConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@Import(PaymentTestConfiguration::class)
class PaymentRecoveryServiceTest(
    @Autowired private val loadPendingPaymentPort: LoadPendingPaymentPort,
    @Autowired private val paymentValidationPort: PaymentValidationPort,
    @Autowired private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    @Autowired private val checkOutUseCase: CheckOutUseCase,
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper,
    @Autowired private val paymentErrorHandler: PaymentErrorHandler
) {

    @BeforeEach
    fun clean() {
        paymentDatabaseHelper.clean().block()
    }

    @Test
    fun `should recovery payments`() {
        val paymentConfirmCommand = createUnknownStatusPaymentEvent()
        val paymentExecutionResult = createPaymentExecutionResult(paymentConfirmCommand)

        val mockPaymentExecutionPort = mockk<PaymentExecutionPort>()

        every { mockPaymentExecutionPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)

        val paymentRecoveryService = PaymentRecoveryService(
            loadPendingPaymentPort,
            paymentValidationPort,
            paymentExecutionPort = mockPaymentExecutionPort,
            paymentStatusUpdatePort,
            paymentErrorHandler
        )
        paymentRecoveryService.recovery()

        Thread.sleep(10000)
    }

    @Test
    fun `should fail recovery when an unknown exception occurs`() {
        val paymentConfirmCommand = createUnknownStatusPaymentEvent()

        val mockPaymentExecutionPort = mockk<PaymentExecutionPort>()

        every { mockPaymentExecutionPort.execute(paymentConfirmCommand) } throws PSPConfirmationException(
            errorCode = "UNKNOWN_ERROR",
            errorMessage = "test_error_message",
            isSuccess = false,
            isFailure = false,
            isUnknown = true,
            isRetryableError = true
        )

        val paymentRecoveryService = PaymentRecoveryService(
            loadPendingPaymentPort,
            paymentValidationPort,
            paymentExecutionPort = mockPaymentExecutionPort,
            paymentStatusUpdatePort,
            paymentErrorHandler
        )
        paymentRecoveryService.recovery()

        Thread.sleep(10000)
    }

    private fun createPaymentExecutionResult(paymentConfirmCommand: PaymentConfirmCommand) =
        PaymentExecutionResult(
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

    private fun createUnknownStatusPaymentEvent(): PaymentConfirmCommand {
        val orderId = UUID.randomUUID().toString()
        val paymentKey = UUID.randomUUID().toString()

        val checkOutCommand = CheckOutCommand(
            cartId = 1L,
            buyerId = 1L,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkOutResult = checkOutUseCase.checkOut(checkOutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = checkOutResult.amount
        )

        paymentStatusUpdatePort.updatePaymentStatusToExecuting(orderId, paymentConfirmCommand.paymentKey).block()

        val paymentStatusUpdateCommand = PaymentStatusUpdateCommand(
            paymentKey = paymentKey,
            orderId = orderId,
            status = PaymentStatus.UNKNOWN,
            failure = PaymentFailure("UNKNOWN", "UNKNOWN")
        )

        paymentStatusUpdatePort.updatePaymentStatus(paymentStatusUpdateCommand).block()
        return paymentConfirmCommand
    }
}