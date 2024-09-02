package org.example.paymentservice.payment.domain

data class CheckOutResult(
    val amount: Long,
    val orderId: String,
    val orderName: String
)