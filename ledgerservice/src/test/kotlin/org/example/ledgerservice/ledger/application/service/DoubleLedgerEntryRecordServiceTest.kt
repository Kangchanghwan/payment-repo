package org.example.ledgerservice.ledger.application.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.example.ledgerservice.ledger.adapter.out.persistence.repository.SpringDataJpaLedgerEntryRepository
import org.example.ledgerservice.ledger.adapter.out.persistence.repository.SpringDataJpaLedgerTransactionRepository
import org.example.ledgerservice.ledger.application.port.out.DuplicateMessageFilterPort
import org.example.ledgerservice.ledger.application.port.out.LoadAccountPort
import org.example.ledgerservice.ledger.application.port.out.LoadPaymentOrderPort
import org.example.ledgerservice.ledger.application.port.out.SaveDoubleLedgerEntryPort
import org.example.ledgerservice.ledger.domain.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class DoubleLedgerEntryRecordServiceTest(
    @Autowired private val springDataJpaLedgerEntryRepository: SpringDataJpaLedgerEntryRepository,
    @Autowired private val springDataJpaLedgerTransactionRepository: SpringDataJpaLedgerTransactionRepository,
    @Autowired private val duplicateMessageFilterPort: DuplicateMessageFilterPort,
    @Autowired private val loadAccountPort: LoadAccountPort,
    @Autowired private val saveDoubleLedgerEntryPort: SaveDoubleLedgerEntryPort
){

    @BeforeEach
    fun clean() {
        springDataJpaLedgerEntryRepository.deleteAll()
        springDataJpaLedgerTransactionRepository.deleteAll()
    }

    @Test
    fun `should record double ledger entries successfully`() {
        val paymentEventMessage = PaymentEventMessage(
            type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
            payload = mapOf(
                "orderId" to UUID.randomUUID().toString(),
            ),
        )

        val mockLoadPaymentOrderPort = mockk<LoadPaymentOrderPort>()

        every { mockLoadPaymentOrderPort.getPaymentOrders(paymentEventMessage.orderId()) } returns listOf(
            PaymentOrder(
                id = 1L,
                amount =  1000L,
                orderId = paymentEventMessage.orderId()
            ),
            PaymentOrder(
                id = 2L,
                amount =  2000L,
                orderId = paymentEventMessage.orderId()
            )

        )

        val doubleLedgerEntryRecordService = DoubleLedgerEntryRecordService(
            duplicateMessageFilterPort = duplicateMessageFilterPort,
            loadPaymentOrderPort = mockLoadPaymentOrderPort,
            loadAccountPort = loadAccountPort,
            saveDoubleLedgerEntryPort = saveDoubleLedgerEntryPort
        )

        val legerEventMessage = doubleLedgerEntryRecordService.recordDoubleLegerEntry(paymentEventMessage)

        val jpaLedgerEntryEntities = springDataJpaLedgerEntryRepository.findAll()

        val sumOf = jpaLedgerEntryEntities.sumOf {
            when (it.type) {
                LedgerEntryType.CREDIT -> it.amount
                LedgerEntryType.DEBIT -> it.amount.negate()
            }
        }

        assertThat(legerEventMessage.type).isEqualTo(LedgerEventMessageType.SUCCESS)
        assertThat(legerEventMessage.payload["orderId"]).isEqualTo(paymentEventMessage.orderId())
        assertThat(sumOf.toInt()).isEqualTo(0)
        assertThat(jpaLedgerEntryEntities.size).isEqualTo(4)
    }

}