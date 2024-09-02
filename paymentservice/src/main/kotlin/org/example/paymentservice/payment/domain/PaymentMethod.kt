package org.example.paymentservice.payment.domain

enum class PaymentMethod(val method: String) {
    CARD("간편결제");

    companion object {
        fun get(method: String): PaymentMethod {
            return entries.find { it.method == method }
                ?: error("PaymentMethod (Method: $method) 은 올바르지 않은 메소드입니다..)")
        }
    }
}
