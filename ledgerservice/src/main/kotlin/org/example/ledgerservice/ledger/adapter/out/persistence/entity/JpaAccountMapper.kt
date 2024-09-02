package org.example.ledgerservice.ledger.adapter.out.persistence.entity

import org.example.ledgerservice.ledger.domain.Account
import org.springframework.stereotype.Component

@Component
class JpaAccountMapper {

    fun mapToDomainEntity(entity: JpaAccountEntity): Account {
        return Account(
            id = entity.id!!,
            name = entity.name
        )
    }
}