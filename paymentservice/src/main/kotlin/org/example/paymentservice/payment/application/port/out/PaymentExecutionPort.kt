package org.example.paymentservice.payment.application.port.out

import org.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import org.example.paymentservice.payment.domain.PaymentExecutionResult
import reactor.core.publisher.Mono

interface PaymentExecutionPort {
    fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult>
}