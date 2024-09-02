package org.example.paymentservice.payment.adapter.out.persistent.repository

import org.example.paymentservice.payment.adapter.out.persistent.util.MySQLDateTimeFormatter
import org.example.paymentservice.payment.domain.*
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

@Repository
class R2DBCPaymentRepository(
    private val databaseClient: DatabaseClient,
    private val transactionOperators: TransactionalOperator,
) : PaymentRepository {
    override fun save(paymentEvent: PaymentEvent): Mono<Void> {
        return insertPaymentEvent(paymentEvent) // 결제 이벤트를 저장합니다.
            .flatMap { selectPaymentEventId() } // 마지막으로 생성된 이벤트 ID 값을 가져옵니다.
            .flatMap { paymentEventId ->
                insertPaymentOrders(
                    paymentEvent,
                    paymentEventId
                )
            } // 결제 이벤트 아이디를 외래키로 가진 결제주문 건을 생성합니다.
            .`as` { transactionOperators.transactional(it) } // 트랜젝션을 묶는 역할
            .then()
    }

    override fun getPendingPayments(): Flux<PendingPaymentEvent> {
        return databaseClient.sql(SELECT_PENDING_PAYMENT_QUERY)
            .bind("updatedAt", LocalDateTime.now().format(MySQLDateTimeFormatter))
            .fetch()
            .all()
            .groupBy { it["payment_event_id"] as Long }
            .flatMap { groupFlux ->
                groupFlux.collectList().map { results ->
                    PendingPaymentEvent(
                        paymentEventId = groupFlux.key(),
                        paymentKey = results.first()["payment_key"] as String,
                        orderId = results.first()["order_id"] as String,
                        pendingPaymentOrders = results.map { result ->
                            PendingPaymentOrder(
                                paymentOrderId = result["payment_order_id"] as Long,
                                status = PaymentStatus.get(result["payment_order_status"] as String),
                                amount = (result["amount"] as BigDecimal).toLong(),
                                failCount = result["failed_count"] as Byte,
                                threshold = result["threshold"] as Byte
                            )
                        }
                    )
                }
            }
    }

    override fun getPayment(orderId: String): Mono<PaymentEvent> {
        return databaseClient.sql(SELETE_PAYMENT_EVENT_QUERY)
            .bind("orderId", orderId)
            .fetch()
            .all()
            .groupBy { it["payment_event_id"] as Long }
            .flatMap { groupedFlux ->
                groupedFlux.collectList().map { results ->
                    val paymentOrders = results.map { result ->
                        PaymentOrder(
                            id = result["payment_order_id"] as Long,
                            paymentEventId = groupedFlux.key(),
                            sellerId = result["seller_id"] as Long,
                            productId = result["product_id"] as Long,
                            orderId = result["order_id"] as String,
                            amount = (result["amount"] as BigDecimal).toLong(),
                            paymentStatus = PaymentStatus.get(result["payment_order_status"] as String),
                            isLegerUpdated = if (((result["ledger_updated"] as Byte).toInt()) == 1) true else false,
                            isWalletUpdated = if (((result["wallet_updated"] as Byte).toInt()) == 1) true else false,
                        )
                    }
                    PaymentEvent(
                        id = groupedFlux.key(),
                        orderId = results.first()["order_id"] as String,
                        buyerId = results.first()["buyer_id"] as Long,
                        orderName = results.first()["order_name"] as String,
                        paymentOrders = paymentOrders,
                        isPaymentDone = if (((results.first()["is_payment_done"] as Byte).toInt()) == 1) true else false
                    )
                }
            }.toMono()
    }

    override fun complete(paymentEvent: PaymentEvent): Mono<Void> {
        return when {
            paymentEvent.isPaymentDone() -> handlePaymentCompletion(paymentEvent)
            paymentEvent.isLedgerUpdateDone() -> handleLedgerUpdate(paymentEvent)
            paymentEvent.isWalletUpdateDone() -> handleWalletUpdate(paymentEvent)
            else -> error("Incorrect state for PaymentEvent id: ${paymentEvent.id}")
        }
    }

    private fun insertPaymentEvent(paymentEvent: PaymentEvent): Mono<Long> {
        return databaseClient.sql(INSERT_PAYMENT_EVENT_QUERY)
            .bind("buyerId", paymentEvent.buyerId)
            .bind("orderName", paymentEvent.orderName)
            .bind("orderId", paymentEvent.orderId)
            .fetch()
            .rowsUpdated()
    }

    private fun selectPaymentEventId() = databaseClient.sql(LAST_INSERT_ID_QUERY)
        .fetch().first().map { (it["LAST_INSERT_ID()"] as BigInteger).toLong() }


    private fun insertPaymentOrders(
        paymentEvent: PaymentEvent,
        paymentEventId: Long,
    ): Mono<Long> {
        val valueClause = paymentEvent.paymentOrders.joinToString(", ") { paymentOrder ->
            "($paymentEventId, ${paymentOrder.sellerId}, '${paymentOrder.orderId}', ${paymentOrder.productId}, ${paymentOrder.amount}, '${paymentOrder.paymentStatus}')"
        }
        return databaseClient.sql(INSERT_PAYMENT_ORDER_QUERY(valueClause))
            .fetch()
            .rowsUpdated()
    }

    private fun handleWalletUpdate(paymentEvent: PaymentEvent): Mono<Void> {
        return databaseClient.sql(UPDATE_PAYMENT_ORDER_WALLET_DONE_QUERY)
            .bind("orderId", paymentEvent.orderId)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun handleLedgerUpdate(paymentEvent: PaymentEvent): Mono<Void> {
        return databaseClient.sql(UPDATE_PAYMENT_ORDER_LEDGER_DONE_QUERY)
            .bind("orderId", paymentEvent.orderId)
            .fetch()
            .rowsUpdated()
            .then()

    }

    private fun handlePaymentCompletion(paymentEvent: PaymentEvent): Mono<Void> {
        return Mono.`when`(
            handleLedgerUpdate(paymentEvent),
            handleWalletUpdate(paymentEvent)
        ).then(Mono.defer { completePaymentEvent(paymentEvent) })
    }

    private fun completePaymentEvent(paymentEvent: PaymentEvent): Mono<Void> {
        return databaseClient.sql(UPDATE_PAYMENT_EVENT_COMPLETE_QUERY)
            .bind("orderId", paymentEvent.orderId)
            .fetch()
            .rowsUpdated()
            .then()
    }

    companion object {
        val INSERT_PAYMENT_EVENT_QUERY = """
            INSERT INTO payment_events(buyer_id, order_name, order_id)
            VALUES (:buyerId, :orderName, :orderId)
        """.trimIndent()

        val LAST_INSERT_ID_QUERY = """
            SELECT LAST_INSERT_ID() 
        """.trimIndent() // LAST_INSERT_ID()는 MySQL에서 사용되는 함수이며

        val INSERT_PAYMENT_ORDER_QUERY = fun(valueClauses: String) = """
            INSERT INTO payment_orders(payment_event_id, seller_id, order_id, product_id, amount, payment_order_status)
            VALUES $valueClauses
        """.trimIndent()

        val SELECT_PENDING_PAYMENT_QUERY = """
            SELECT pe.id as payment_event_id, pe.payment_key, pe.order_id, po.id as payment_order_id, po.payment_order_status, po.amount, po.failed_count, po.threshold
            FROM payment_events pe
            INNER JOIN payment_orders po on pe.order_id = po.order_id
            WHERE (po.payment_order_status = 'UNKNOWN' OR (po.payment_order_status = 'EXECUTING' AND  po.updated_at <= :updatedAt - INTERVAL 3 MINUTE))
            AND po.failed_count < po.threshold
            LIMIT 10
        """.trimIndent()

        val SELETE_PAYMENT_EVENT_QUERY = """
            SELECT pe.id as payment_event_id, po.id as payment_order_id, pe.order_id, pe.order_name, pe.buyer_id, pe.is_payment_done, po.seller_id, po.product_id, po.payment_order_status, po.amount, po.ledger_updated, po.wallet_updated
            FROM payment_events pe
            INNER JOIN payment_orders po ON pe.order_id = po.order_id
            WHERE pe.order_id = :orderId
        """.trimIndent()

        val UPDATE_PAYMENT_ORDER_LEDGER_DONE_QUERY = """
            UPDATE payment_orders 
            SET ledger_updated = true 
            WHERE order_id = :orderId 
        """.trimIndent()

        val UPDATE_PAYMENT_ORDER_WALLET_DONE_QUERY = """
            UPDATE payment_orders 
            SET wallet_updated = true 
            WHERE order_id = :orderId 
        """.trimIndent()

        val UPDATE_PAYMENT_EVENT_COMPLETE_QUERY = """
            UPDATE payment_events 
            SET is_payment_done = true 
            WHERE order_id = :orderId 
        """.trimIndent()

    }
}