package org.example.paymentservice.payment.application.port.`in`

import org.example.paymentservice.common.UseCase
import org.example.paymentservice.payment.domain.PaymentConfirmationResult
import reactor.core.publisher.Mono

interface PaymentConfirmUseCase {

    fun confirm(command: PaymentConfirmCommand): Mono<PaymentConfirmationResult>
}