package dsmhackathon18.yesandaero.domain.store.service

import dsmhackathon18.yesandaero.domain.store.dto.MenuResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreDetailResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterResponse
import dsmhackathon18.yesandaero.domain.store.entity.Menu
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.exception.StoreAlreadyExistsException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.MenuRepository
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import dsmhackathon18.yesandaero.global.util.GeoDistanceCalculator
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

    @Transactional(readOnly = true)
    fun getStoreDetail(storeId: Long, lat: Double?, lng: Double?): StoreDetailResponse {
        val store = storeRepository.findById(storeId).orElseThrow { StoreNotFoundException() }
        return buildDetailResponse(store, lat, lng)
    }

    @Transactional(readOnly = true)
    fun getMyStore(ownerUserId: Long): StoreDetailResponse {
        val store = storeRepository.findByOwnerUserId(ownerUserId) ?: throw StoreNotFoundException()
        return buildDetailResponse(store, lat = null, lng = null)
    }

    private fun buildDetailResponse(store: Store, lat: Double?, lng: Double?): StoreDetailResponse {
        val menus = menuRepository.findByStoreIdOrderByDisplayOrderAsc(requireNotNull(store.id))
            .map(MenuResponse::from)
        val distance = GeoDistanceCalculator.calculate(lat, lng, store.latitude, store.longitude)
        // TODO: coupon 도메인 완성 후 로그인한 사용자의 실제 사용 가능 쿠폰 수로 대체
        return StoreDetailResponse.of(store, menus, distance, usableCouponCount = 0)
    }
}
