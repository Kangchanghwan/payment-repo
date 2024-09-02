package org.example.ledgerservice.ledger.adapter.out.persistence.entity

import org.example.ledgerservice.ledger.domain.DoubleLedgerEntry
import org.springframework.stereotype.Component

@Component
class JpaLedgerEntryMapper(
    private val jpaLedgerTransactionMapper: JpaLedgerTransactionMapper
) {
    fun mapToEntity(doubleLedgerEntity: DoubleLedgerEntry) : List<JpaLedgerEntryEntity> {
        val jpaLedgerTransactionEntity = jpaLedgerTransactionMapper.mapToJpaEntity(doubleLedgerEntity.transaction)
        return listOf(
            JpaLedgerEntryEntity(
                amount = doubleLedgerEntity.credit.amount.toBigDecimal(),
                accountId = doubleLedgerEntity.credit.account.id,
                type = doubleLedgerEntity.credit.type,
                transaction = jpaLedgerTransactionEntity
            ),
            JpaLedgerEntryEntity(
                amount = doubleLedgerEntity.debit.amount.toBigDecimal(),
                accountId = doubleLedgerEntity.debit.account.id,
                type = doubleLedgerEntity.debit.type,
                transaction = jpaLedgerTransactionEntity
            )
        )
    }
}