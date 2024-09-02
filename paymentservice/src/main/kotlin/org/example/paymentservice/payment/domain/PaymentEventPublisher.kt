package org.example.paymentservice.payment.domain

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalEventPublisher
import reactor.core.publisher.Mono

@Component
class PaymentEventPublisher(
    publisher: ApplicationEventPublisher
) {
    private val transactionalEventPublisher = TransactionalEventPublisher(publisher) // 트랜젝션이 커밋된 후에 발행하는 객체

    fun publishEvent(paymentEventMessage: PaymentEventMessage): Mono<PaymentEventMessage> {
        return transactionalEventPublisher.publishEvent(paymentEventMessage)
            .thenReturn(paymentEventMessage)
    }

}