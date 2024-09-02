package org.example.paymentservice.payment.application.service

import org.assertj.core.api.Assertions.*
import org.example.paymentservice.payment.application.port.`in`.CheckOutCommand
import org.example.paymentservice.payment.application.port.`in`.CheckOutUseCase
import org.example.paymentservice.payment.test.PaymentDatabaseHelper
import org.example.paymentservice.payment.test.PaymentTestConfiguration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import reactor.test.StepVerifier
import java.util.*

@SpringBootTest
@Import(PaymentTestConfiguration::class)
class CheckOutServiceTest(
    @Autowired private val checkOutUseCase: CheckOutUseCase,
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper,
) {

    @BeforeEach
    fun setUp() {
        paymentDatabaseHelper.clean().block()
    }

    @Test
    fun `should save PaymentEvent and PaymentOrder successfully`() {
        val orderId = UUID.randomUUID().toString()
        val checkOutCommand = CheckOutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        StepVerifier.create(checkOutUseCase.checkOut(checkOutCommand))
            .expectNextMatches { it.amount.toInt() == 60_000 && it.orderId == orderId }
            .verifyComplete()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentEvent.orderId).isEqualTo(orderId)
        assertThat(paymentEvent.totalAmount()).isEqualTo(60_000)
        assertThat(paymentEvent.paymentOrders.size).isEqualTo(checkOutCommand.productIds.size)
        assertFalse(paymentEvent.isPaymentDone())
        assertTrue(paymentEvent.paymentOrders.all { !it.isLegerUpdated() })
        assertTrue(paymentEvent.paymentOrders.all { !it.isLegerUpdated() })
    }
    @Test
    fun `should fail to save PaymentEvent and PaymentOrder when trying to save for the second time`() {
        val orderId = UUID.randomUUID().toString()
        val checkOutCommand = CheckOutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        checkOutUseCase.checkOut(checkOutCommand).block()

        assertThrows<DataIntegrityViolationException> {
            checkOutUseCase.checkOut(checkOutCommand).block()
        }
    }

}