package org.example.paymentservice.payment.adapter.out.web.toss.repsonse


data class TossPaymentConfirmResponse(
    val version: String,
    val paymentKey: String,
    val type: String,
    val orderId: String,
    val orderName: String,
    val mId: String,
    val currency: String,
    val method: String,
    val totalAmount: Int,
    val balanceAmount: Int,
    val status: String,
    val requestedAt: String,
    val approvedAt: String,
    val useEscrow: Boolean,
    val lastTransactionKey: String?,
    val suppliedAmount: Int,
    val vat: Int,
    val cultureExpense: Boolean,
    val taxFreeAmount: Int,
    val taxExemptionAmount: Int,
    val cancels: List<Cancel>?, // 대체 필요
    val card: Card?, // 대체 필요
    val virtualAccount: VirtualAccount?, // 대체 필요
    val mobilePhone: MobilePhone?, // 대체 필요
    val giftCertificate: GiftCertificate?, // 대체 필요
    val transfer: Transfer?, // 대체 필요
    val receipt: Receipt?, // 대체 필요
    val checkout: Checkout?, // 대체 필요
    val easyPay: EasyPay?, // 대체 필요
    val country: String,
    val tossFailureResponse: TossFailureResponse?, // 대체 필요
    val cashReceipt: CashReceipt?, // 대체 필요
    val discount: Discount? // 대체 필요
)

data class RefundReceiveAccount(
    val bankCode: String,
    val accountNumber: String,
    val holderName: String
)

data class VirtualAccount(
    val accountType: String,
    val accountNumber: String,
    val bankCode: String,
    val customerName: String,
    val dueDate: String,
    val refundStatus: String,
    val expired: Boolean,
    val settlementStatus: String,
    val refundReceiveAccount: RefundReceiveAccount?
)

data class CustomerMobilePhone(
    val plain: String,
    val masking: String
)

data class MobilePhone(
    val customerMobilePhone: CustomerMobilePhone,
    val settlementStatus: String,
    val receiptUrl: String
)

data class GiftCertificate(
    val approveNo: String,
    val settlementStatus: String
)

data class Transfer(
    val bankCode: String,
    val settlementStatus: String
)

data class Cancel(
    val cancelAmount: Number,
    val cancelReason: String,
    val taxFreeAmount: Number,
    val taxExemptionAmount: Int,
    val refundableAmount: Number,
    val easyPayDiscountAmount: Number,
    val canceledAt: String,
    val transactionKey: String,
    val receiptKey: String?,
    val cancelStatus: String,
    val cancelRequestId: String?
)

data class Card(
    val issuerCode: String,
    val acquirerCode: String,
    val number: String,
    val installmentPlanMonths: Int,
    val isInterestFree: Boolean,
    val interestPayer: Any?,
    val approveNo: String,
    val useCardPoint: Boolean,
    val cardType: String,
    val ownerType: String,
    val acquireStatus: String,
    val receiptUrl: String,
    val amount: Int
)

data class Receipt(
    val url: String
)

data class Checkout(
    val url: String
)

data class TossFailureResponse(
    val code: String,
    val message: String
)

data class CashReceipt(
    val type: String,
    val receiptKey: String,
    val issueNumber: String,
    val receiptUrl: String,
    val amount: Number,
    val taxFreeAmount: Number,
    val orderId: String,
    val orderName: String,
    val businessNumber: String,
    val transactionType: String,
    val issueStatus: String,
    val tossFailureResponse: TossFailureResponse?,
    val customerIdentityNumber: String,
    val requestedAt: String
)

data class Discount(
    val amount: Int
)

data class EasyPay(
    val provider: String,
    val amount: Int,
    val discountAmount: Int
)
