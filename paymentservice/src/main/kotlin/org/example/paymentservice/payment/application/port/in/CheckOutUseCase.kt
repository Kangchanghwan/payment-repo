package org.example.paymentservice.payment.application.port.`in`

import org.example.paymentservice.common.UseCase
import org.example.paymentservice.payment.domain.CheckOutResult
import reactor.core.publisher.Mono

interface CheckOutUseCase {
    fun checkOut(command: CheckOutCommand): Mono<CheckOutResult>
}