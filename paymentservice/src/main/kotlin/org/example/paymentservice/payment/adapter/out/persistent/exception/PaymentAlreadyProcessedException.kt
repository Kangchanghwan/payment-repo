package org.example.paymentservice.payment.adapter.out.persistent.exception

import org.example.paymentservice.payment.domain.PaymentStatus

class PaymentAlreadyProcessedException(
    val status: PaymentStatus,
    message: String
): RuntimeException(message)