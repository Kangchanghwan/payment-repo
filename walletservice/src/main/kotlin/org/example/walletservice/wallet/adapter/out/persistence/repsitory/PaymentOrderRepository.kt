package org.example.walletservice.wallet.adapter.out.persistence.repsitory

import org.example.walletservice.wallet.domain.PaymentOrder

interface PaymentOrderRepository {
    fun getPaymentOrders(orderId: String): List<PaymentOrder>
}