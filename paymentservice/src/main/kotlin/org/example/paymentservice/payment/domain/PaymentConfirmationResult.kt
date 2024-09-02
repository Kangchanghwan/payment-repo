package org.example.paymentservice.payment.domain

data class PaymentConfirmationResult (
    val status: PaymentStatus,
    val failure: PaymentFailure? = null
){
    init {
        if(status == PaymentStatus.FAILURE){
            requireNotNull(failure) {
                "Failure reason must be provided for PaymentStatus.FAILURE"
            }
        }
    }
    val message = when(status) {
        PaymentStatus.SUCCESS -> "결제처리에 성공하였습니다."
        PaymentStatus.FAILURE -> "결제처리에 실패하였습니다."
        PaymentStatus.UNKNOWN -> "결제처리 중에 알 수 없는 에러가 발생하였습니다."
        else -> error("현재 결제 상태(status : $status)가 올바르지 않습니다.")
    }
}
