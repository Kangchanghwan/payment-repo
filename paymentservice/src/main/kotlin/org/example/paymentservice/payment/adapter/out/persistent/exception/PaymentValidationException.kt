package org.example.paymentservice.payment.adapter.out.persistent.exception

import org.example.paymentservice.payment.domain.PaymentStatus

class PaymentValidationException(
    message: String
): RuntimeException(message)