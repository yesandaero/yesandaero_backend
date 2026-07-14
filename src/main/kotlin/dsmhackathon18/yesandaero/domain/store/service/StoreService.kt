package dsmhackathon18.yesandaero.domain.store.service

import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus as PartnershipEntityStatus
import dsmhackathon18.yesandaero.domain.partnership.repository.PartnershipRepository
import dsmhackathon18.yesandaero.domain.store.dto.CategoryListResponse
import dsmhackathon18.yesandaero.domain.store.dto.MenuBulkUpdateRequest
import dsmhackathon18.yesandaero.domain.store.dto.MenuBulkUpdateResponse
import dsmhackathon18.yesandaero.domain.store.dto.MenuResponse
import dsmhackathon18.yesandaero.domain.store.dto.PartnershipStatus
import dsmhackathon18.yesandaero.domain.store.dto.StoreDetailResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreListResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreListSort
import dsmhackathon18.yesandaero.domain.store.dto.StoreMapResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreSearchItemResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreSearchListResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreSummaryResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreUpdateRequest
import dsmhackathon18.yesandaero.domain.store.entity.Menu
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreAlreadyExistsException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.MenuRepository
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import dsmhackathon18.yesandaero.global.exception.InvalidRequestException
import dsmhackathon18.yesandaero.global.util.GeoDistanceCalculator
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoreService(
    private val storeRepository: StoreRepository,
    private val menuRepository: MenuRepository,
    private val partnershipRepository: PartnershipRepository,
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

    @Transactional
    fun updateStore(ownerUserId: Long, storeId: Long, request: StoreUpdateRequest): StoreDetailResponse {
        val store = storeRepository.findById(storeId).orElseThrow { StoreNotFoundException() }
        if (store.ownerUserId != ownerUserId) {
            throw NotStoreOwnerException()
        }

        store.update(
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
        )

        return buildDetailResponse(store, lat = null, lng = null)
    }

    @Transactional
    fun replaceMenus(ownerUserId: Long, storeId: Long, request: MenuBulkUpdateRequest): MenuBulkUpdateResponse {
        val store = storeRepository.findById(storeId).orElseThrow { StoreNotFoundException() }
        if (store.ownerUserId != ownerUserId) {
            throw NotStoreOwnerException()
        }

        menuRepository.deleteAllByStoreId(storeId)

        val newMenus = request.menus.mapIndexed { index, menuRequest ->
            Menu(
                store = store,
                name = menuRequest.name,
                description = menuRequest.description,
                price = menuRequest.price,
                discountedPrice = menuRequest.discountedPrice,
                displayOrder = index,
            )
        }
        val saved = if (newMenus.isNotEmpty()) menuRepository.saveAll(newMenus) else emptyList()

        return MenuBulkUpdateResponse(menus = saved.map(MenuResponse::from))
    }

    fun getCategories(): CategoryListResponse = CategoryListResponse.all()

    @Transactional(readOnly = true)
    fun listStores(
        categories: List<StoreCategory>?,
        maxPrice: Int?,
        lat: Double?,
        lng: Double?,
        sort: StoreListSort?,
        page: Int,
        size: Int,
    ): StoreListResponse {
        if (maxPrice != null && maxPrice < 0) {
            throw InvalidRequestException("maxPrice는 0 이상이어야 합니다")
        }

        val effectiveCategories = categories?.takeIf { it.isNotEmpty() } ?: StoreCategory.entries.toList()
        val effectiveSort = sort ?: (if (lat != null && lng != null) StoreListSort.DISTANCE_ASC else null)

        if (effectiveSort == StoreListSort.DISTANCE_ASC && (lat == null || lng == null)) {
            throw InvalidRequestException("DISTANCE_ASC 정렬에는 lat, lng가 필요합니다")
        }

        val storePage: Page<Store> = when (effectiveSort) {
            StoreListSort.PRICE_ASC -> storeRepository.findAllByFilters(
                effectiveCategories,
                maxPrice,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "avgPrice")),
            )
            StoreListSort.DISCOUNT_DESC -> storeRepository.findAllByFiltersOrderByDiscountDesc(
                effectiveCategories,
                maxPrice,
                PageRequest.of(page, size),
            )
            StoreListSort.DISTANCE_ASC -> distanceSortedPage(
                effectiveCategories,
                maxPrice,
                requireNotNull(lat),
                requireNotNull(lng),
                PageRequest.of(page, size),
            )
            null -> storeRepository.findAllByFilters(
                effectiveCategories,
                maxPrice,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
            )
        }

        val content = storePage.content.map { store ->
            val distance = GeoDistanceCalculator.calculate(lat, lng, store.latitude, store.longitude)
            // TODO: coupon 도메인 완성 후 실제 사용 가능 쿠폰 보유 여부로 대체
            StoreSummaryResponse.of(store, distance, hasUsableCoupon = false)
        }

        return StoreListResponse(content = content, page = storePage.number, totalPages = storePage.totalPages)
    }

    @Transactional(readOnly = true)
    fun getStoresInBounds(
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double,
        maxPrice: Int?,
        categories: List<StoreCategory>?,
        limit: Int,
        lat: Double?,
        lng: Double?,
    ): StoreMapResponse {
        if (swLat > neLat || swLng > neLng) {
            throw InvalidRequestException("바운딩 박스 좌표 범위가 올바르지 않습니다")
        }
        if (maxPrice != null && maxPrice < 0) {
            throw InvalidRequestException("maxPrice는 0 이상이어야 합니다")
        }

        val effectiveCategories = categories?.takeIf { it.isNotEmpty() } ?: StoreCategory.entries.toList()
        var stores = storeRepository.findAllInBoundingBox(swLat, swLng, neLat, neLng, effectiveCategories, maxPrice)

        if (lat != null && lng != null) {
            stores = stores.sortedBy { GeoDistanceCalculator.distanceMeters(lat, lng, it.latitude, it.longitude) }
        }

        val totalInBounds = stores.size
        val truncated = totalInBounds > limit

        val content = stores.take(limit).map { store ->
            val distance = GeoDistanceCalculator.calculate(lat, lng, store.latitude, store.longitude)
            // TODO: coupon 도메인 완성 후 실제 사용 가능 쿠폰 보유 여부로 대체
            StoreSummaryResponse.of(store, distance, hasUsableCoupon = false)
        }

        return StoreMapResponse(stores = content, totalInBounds = totalInBounds, truncated = truncated)
    }

    @Transactional(readOnly = true)
    fun searchStoresForPartnership(
        ownerUserId: Long,
        keyword: String,
        category: StoreCategory?,
        page: Int,
        size: Int,
    ): StoreSearchListResponse {
        val myStore = storeRepository.findByOwnerUserId(ownerUserId) ?: throw StoreNotFoundException()
        val myStoreId = requireNotNull(myStore.id)

        val storePage = storeRepository.searchForPartnership(ownerUserId, keyword, category, PageRequest.of(page, size))
        val targetStoreIds = storePage.content.mapNotNull { it.id }

        val partnershipByPartnerStoreId = if (targetStoreIds.isEmpty()) {
            emptyMap()
        } else {
            partnershipRepository.findAllBetween(myStoreId, targetStoreIds)
                .associateBy { if (it.requesterStoreId == myStoreId) it.receiverStoreId else it.requesterStoreId }
        }

        val content = storePage.content.map { store ->
            val partnership = partnershipByPartnerStoreId[store.id]
            val partnershipStatus = when (partnership?.status) {
                PartnershipEntityStatus.PENDING -> PartnershipStatus.PENDING
                PartnershipEntityStatus.ACCEPTED -> PartnershipStatus.ACCEPTED
                // REJECTED/TERMINATED/없음은 재요청 가능해야 하므로 NONE 취급
                else -> PartnershipStatus.NONE
            }
            StoreSearchItemResponse(
                storeId = requireNotNull(store.id),
                name = store.name,
                category = store.category,
                partnershipStatus = partnershipStatus,
            )
        }

        return StoreSearchListResponse(content = content, page = storePage.number, totalPages = storePage.totalPages)
    }

    private fun distanceSortedPage(
        categories: List<StoreCategory>,
        maxPrice: Int?,
        lat: Double,
        lng: Double,
        pageable: Pageable,
    ): Page<Store> =
        storeRepository.findAllByFiltersOrderByDistanceAsc(
            categories.map { it.name },
            maxPrice,
            lat,
            lng,
            pageable,
        )

    private fun buildDetailResponse(store: Store, lat: Double?, lng: Double?): StoreDetailResponse {
        val menus = menuRepository.findByStoreIdOrderByDisplayOrderAsc(requireNotNull(store.id))
            .map(MenuResponse::from)
        val distance = GeoDistanceCalculator.calculate(lat, lng, store.latitude, store.longitude)
        // TODO: coupon 도메인 완성 후 로그인한 사용자의 실제 사용 가능 쿠폰 수로 대체
        return StoreDetailResponse.of(store, menus, distance, usableCouponCount = 0)
    }
}
