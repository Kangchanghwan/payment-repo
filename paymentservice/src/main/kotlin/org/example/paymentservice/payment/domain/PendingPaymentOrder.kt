package org.example.paymentservice.payment.domain

data class PendingPaymentOrder(
    val paymentOrderId: Long,
    val status: PaymentStatus,
    val amount: Long,
    val failCount: Byte,
    val threshold: Byte
)