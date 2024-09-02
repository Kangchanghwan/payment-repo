package org.example.paymentservice.payment.application.port.out

import org.example.paymentservice.payment.domain.PaymentExecutionResult
import org.example.paymentservice.payment.domain.PaymentExtraDetails
import org.example.paymentservice.payment.domain.PaymentFailure
import org.example.paymentservice.payment.domain.PaymentStatus

data class PaymentStatusUpdateCommand(
    val paymentKey: String,
    val orderId: String,
    val status:PaymentStatus,
    val extraDetails: PaymentExtraDetails? = null,
    val failure: PaymentFailure? = null
){
    constructor(paymentExecutionResult: PaymentExecutionResult): this(
        paymentKey = paymentExecutionResult.paymentKey,
        orderId = paymentExecutionResult.orderId,
        status = paymentExecutionResult.paymentStatus(),
        extraDetails = paymentExecutionResult.extraDetails,
        failure = paymentExecutionResult.failure,
    )

    init {
        require(status == PaymentStatus.SUCCESS || status== PaymentStatus.FAILURE || status == PaymentStatus.UNKNOWN){
            "결제 상태 (status: ${status}) 는 올바르지 않은 결제 상태입니다."
        }
        if(status == PaymentStatus.SUCCESS) {
            requireNotNull(extraDetails) {
                "결제 상태가 성공인 경우 추가 세부 정보(extraDetails)는 필수입니다."
            }
        }
        if(status == PaymentStatus.FAILURE) {
            requireNotNull(failure) {
                "결제 상태가 실패인 경우 실패 정보(failure)는 필수입니다."
            }
        }
    }
}