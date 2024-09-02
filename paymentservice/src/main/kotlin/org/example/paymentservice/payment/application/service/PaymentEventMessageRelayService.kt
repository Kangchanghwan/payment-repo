package org.example.paymentservice.payment.application.service

import org.example.paymentservice.common.Logger
import org.example.paymentservice.common.UseCase
import org.example.paymentservice.payment.application.port.`in`.PaymentEventMessageRelayUseCase
import org.example.paymentservice.payment.application.port.out.DispatchEventMessagePort
import org.example.paymentservice.payment.application.port.out.LoadPendingPaymentEventMessagePort
import org.springframework.scheduling.annotation.Scheduled
import reactor.core.scheduler.Schedulers
import java.util.concurrent.TimeUnit

@UseCase
class PaymentEventMessageRelayService(
    private val loadPendingPaymentEventMessagePort: LoadPendingPaymentEventMessagePort,
    private val dispatchEventMessagePort: DispatchEventMessagePort,
) : PaymentEventMessageRelayUseCase {

    private val scheduler = Schedulers.newSingle("message-relay")

    @Scheduled(fixedDelay = 60, initialDelay = 60, timeUnit = TimeUnit.SECONDS)
    override fun relay() {
        loadPendingPaymentEventMessagePort.getPendingPaymentEventMessage()
            .map { dispatchEventMessagePort.dispatch(it) }
            .onErrorContinue { err, _ ->
                Logger.error(
                    "messageRelay",
                    err.message ?: "fail to relay message.",
                    err
                )
            }
            .subscribeOn(scheduler)
            .subscribe()
    }

}