package org.example.walletservice.wallet.application.service

import org.example.walletservice.common.UseCase
import org.example.walletservice.wallet.application.port.`in`.SettlementUseCase
import org.example.walletservice.wallet.application.port.out.DuplicateMessageFilterPort
import org.example.walletservice.wallet.application.port.out.LoadPaymentOrderPort
import org.example.walletservice.wallet.application.port.out.LoadWallerPort
import org.example.walletservice.wallet.application.port.out.SaveWalletPort
import org.example.walletservice.wallet.domain.*

@UseCase
class SettlementService(
    private val duplicateMessageFilterPort: DuplicateMessageFilterPort,
    private val loadPaymentOrderPort: LoadPaymentOrderPort,
    private val loadWallerPort: LoadWallerPort,
    private val saveWalletPort: SaveWalletPort
) : SettlementUseCase {
    override fun processSettlement(paymentEventMessage: PaymentEventMessage): WalletEventMessage {
        if (duplicateMessageFilterPort.isAlreadyProcess(paymentEventMessage)) {
            return createWalletEventMessage(paymentEventMessage)
        }
        val paymentOrders = loadPaymentOrderPort.getPaymentOrders(paymentEventMessage.orderId())
        val paymentOrdersBySellerId = paymentOrders.groupBy { it.sellerId }

        val updateWallets = getUpdateWallets(paymentOrdersBySellerId)

        saveWalletPort.save(updateWallets)

        return createWalletEventMessage(paymentEventMessage)
    }


    private fun createWalletEventMessage(paymentEventMessage: PaymentEventMessage) =
        WalletEventMessage(
            type = WalletEventMessageType.SUCCESS,
            payload = mapOf(
                "orderId" to paymentEventMessage.orderId()
            )
        )


    private fun getUpdateWallets(paymentOrdersBySellerId: Map<Long, List<PaymentOrder>>): List<Wallet> {
        val sellerIds = paymentOrdersBySellerId.keys

        val wallets = loadWallerPort.getWallets(sellerIds)

        return wallets.map { wallet ->
            wallet.calculateBalanceWith(paymentOrdersBySellerId[wallet.userId]!!)
        }
    }
}