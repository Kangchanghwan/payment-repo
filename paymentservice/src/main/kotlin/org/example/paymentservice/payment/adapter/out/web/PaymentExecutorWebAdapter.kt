package org.example.paymentservice.payment.adapter.out.web

import org.example.paymentservice.common.WebAdapter
import org.example.paymentservice.payment.adapter.out.web.toss.executor.PaymentExecutor
import org.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import org.example.paymentservice.payment.application.port.out.PaymentExecutionPort
import org.example.paymentservice.payment.domain.PaymentExecutionResult
import reactor.core.publisher.Mono

@WebAdapter
class PaymentExecutorWebAdapter(
    private val paymentExecutor: PaymentExecutor
) : PaymentExecutionPort {
    override fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult> {
        return paymentExecutor.execute(command)
    }

}