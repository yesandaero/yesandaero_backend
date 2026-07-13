package dsmhackathon18.yesandaero.domain.store.repository

import dsmhackathon18.yesandaero.domain.store.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

interface StoreRepository : JpaRepository<Store, Long> {

    fun existsByOwnerUserId(ownerUserId: Long): Boolean

    fun findByOwnerUserId(ownerUserId: Long): Store?
}
