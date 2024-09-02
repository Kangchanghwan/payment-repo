package org.example.ledgerservice.ledger.adapter.out.persistence

import org.example.ledgerservice.common.PersistenceAdapter
import org.example.ledgerservice.ledger.adapter.out.persistence.repository.JpaAccountRepository
import org.example.ledgerservice.ledger.application.port.out.LoadAccountPort
import org.example.ledgerservice.ledger.domain.DoubleAccountsForLedger
import org.example.ledgerservice.ledger.domain.FinanceType

@PersistenceAdapter
class AccountPersistAdapter(
    private val accountRepository: JpaAccountRepository
) : LoadAccountPort{
    override fun getDoubleAccountsForLedger(financeType: FinanceType): DoubleAccountsForLedger {
        return accountRepository.getDoubleAccountForLedger(financeType)
    }
}