package org.example.paymentservice.payment.application.service

import org.example.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository
import org.example.paymentservice.payment.application.port.`in`.PaymentEventMessageRelayUseCase
import org.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import org.example.paymentservice.payment.domain.*
import org.example.paymentservice.payment.test.PaymentDatabaseHelper
import org.example.paymentservice.payment.test.PaymentTestConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Hooks
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@Import(PaymentTestConfiguration::class)
class PaymentEventMessageRelayServiceTest(
    @Autowired private val paymentOutboxRepository: PaymentOutboxRepository,
    @Autowired private val paymentEventMessageRelayUseCase: PaymentEventMessageRelayUseCase,
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper,
) {


    @BeforeEach
    fun setup(){
        paymentDatabaseHelper.clean().block()
    }

    @Test
    fun `should dispatch external message system`() {
        Hooks.onOperatorDebug()

        val command = PaymentStatusUpdateCommand(
            paymentExecutionResult = PaymentExecutionResult(
                paymentKey = UUID.randomUUID().toString(),
                orderId = UUID.randomUUID().toString(),
                extraDetails = PaymentExtraDetails(
                    type = PaymentType.NORMAL,
                    method = PaymentMethod.CARD,
                    approvedAt = LocalDateTime.now(),
                    orderName = "test_order_name",
                    pspConfirmationStatus = PSPConfirmationsStatus.DONE,
                    totalAmount = 50000L,
                    pspRawData = "{}"
                ),
                isSuccess = true,
                isFailure = false,
                isUnknown = false,
                isRetryable = false
            )
        )
        paymentOutboxRepository.insertOutbox(command).block()
        paymentEventMessageRelayUseCase.relay()
        Thread.sleep(10000)
    }
}