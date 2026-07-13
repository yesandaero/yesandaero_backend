package dsmhackathon18.yesandaero.domain.store.service

import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterResponse
import dsmhackathon18.yesandaero.domain.store.entity.Menu
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.exception.StoreAlreadyExistsException
import dsmhackathon18.yesandaero.domain.store.repository.MenuRepository
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoreService(
    private val storeRepository: StoreRepository,
    private val menuRepository: MenuRepository,
) {

    @Transactional
    fun registerStore(ownerUserId: Long, request: StoreRegisterRequest): StoreRegisterResponse {
        if (storeRepository.existsByOwnerUserId(ownerUserId)) {
            throw StoreAlreadyExistsException()
        }

        val store = storeRepository.save(
            Store(
                ownerUserId = ownerUserId,
                name = request.name,
                category = request.category,
                address = request.address,
                phone = request.phone,
                avgPrice = request.avgPrice,
                description = request.description,
                latitude = request.latitude,
                longitude = request.longitude,
                openTime = request.openTime,
                closeTime = request.closeTime,
                minOrderAmount = request.minOrderAmount,
            ),
        )

        if (request.menus.isNotEmpty()) {
            val menus = request.menus.mapIndexed { index, menuRequest ->
                Menu(
                    store = store,
                    name = menuRequest.name,
                    description = menuRequest.description,
                    price = menuRequest.price,
                    discountedPrice = menuRequest.discountedPrice,
                    displayOrder = index,
                )
            }
            menuRepository.saveAll(menus)
        }

        return StoreRegisterResponse(storeId = requireNotNull(store.id))
    }
}
