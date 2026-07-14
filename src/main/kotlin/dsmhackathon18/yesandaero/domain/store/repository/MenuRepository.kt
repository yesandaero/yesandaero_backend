package dsmhackathon18.yesandaero.domain.store.repository

import dsmhackathon18.yesandaero.domain.store.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRepository : JpaRepository<Menu, Long> {

    fun findByStoreIdOrderByDisplayOrderAsc(storeId: Long): List<Menu>

    fun deleteAllByStoreId(storeId: Long)
}
