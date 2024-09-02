package org.example.paymentservice.payment.adapter.`in`.web.view

import org.example.paymentservice.common.IdempotencyCreator
import org.example.paymentservice.common.WebAdapter
import org.example.paymentservice.payment.adapter.`in`.web.request.CheckOutRequest
import org.example.paymentservice.payment.application.port.`in`.CheckOutCommand
import org.example.paymentservice.payment.application.port.`in`.CheckOutUseCase
import org.example.paymentservice.payment.application.service.CheckOutService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
@WebAdapter
class CheckOutController(
    private val checkOutUseCase: CheckOutUseCase
) {

    @GetMapping("/")
    fun checkOut(request: CheckOutRequest, model: Model): Mono<String> {
        val command = CheckOutCommand(
            cartId = request.cartId,
            buyerId = request.buyerId,
            productIds = request.productIds,
            idempotencyKey = IdempotencyCreator.create(request.seed)
        )
        return checkOutUseCase.checkOut(command)
            .map {
                model.addAttribute("orderId", it.orderId)
                model.addAttribute("orderName", it.orderName)
                model.addAttribute("amount", it.amount)
                "checkout"
            }
    }
}