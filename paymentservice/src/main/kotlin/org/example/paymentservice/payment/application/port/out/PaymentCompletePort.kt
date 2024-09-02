package org.example.paymentservice.payment.application.port.out

import org.example.paymentservice.payment.domain.PaymentEvent
import reactor.core.publisher.Mono

interface PaymentCompletePort {
    fun complete(paymentEvent: PaymentEvent): Mono<Void>
}