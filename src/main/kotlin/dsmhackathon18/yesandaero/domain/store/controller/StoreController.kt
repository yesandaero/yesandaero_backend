package dsmhackathon18.yesandaero.domain.store.controller

import dsmhackathon18.yesandaero.domain.store.dto.CategoryListResponse
import dsmhackathon18.yesandaero.domain.store.dto.MenuBulkUpdateRequest
import dsmhackathon18.yesandaero.domain.store.dto.MenuBulkUpdateResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreDetailResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreListResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreListSort
import dsmhackathon18.yesandaero.domain.store.dto.StoreMapResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreSearchListResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreUpdateRequest
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.service.StoreService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/stores")
class StoreController(
    private val storeService: StoreService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    fun registerStore(
        @AuthenticationPrincipal ownerUserId: Long,
        @Valid @RequestBody request: StoreRegisterRequest,
    ): StoreRegisterResponse = storeService.registerStore(ownerUserId, request)

    @GetMapping("/me")
    @PreAuthorize("hasRole('OWNER')")
    fun getMyStore(@AuthenticationPrincipal ownerUserId: Long): StoreDetailResponse =
        storeService.getMyStore(ownerUserId)

    @GetMapping("/categories")
    fun getCategories(): CategoryListResponse = storeService.getCategories()

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    fun listStores(
        @RequestParam(required = false) category: List<StoreCategory>?,
        @RequestParam(required = false) maxPrice: Int?,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false) sort: StoreListSort?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): StoreListResponse = storeService.listStores(category, maxPrice, lat, lng, sort, page, size)

    @GetMapping("/map")
    @PreAuthorize("hasRole('CUSTOMER')")
    fun getStoresInBounds(
        @AuthenticationPrincipal userId: Long,
        @RequestParam swLat: Double,
        @RequestParam swLng: Double,
        @RequestParam neLat: Double,
        @RequestParam neLng: Double,
        @RequestParam(required = false) maxPrice: Int?,
        @RequestParam(required = false) category: List<StoreCategory>?,
        @RequestParam(defaultValue = "100") limit: Int,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
    ): StoreMapResponse =
        storeService.getStoresInBounds(swLat, swLng, neLat, neLng, maxPrice, category, limit, lat, lng, userId)

    @GetMapping("/{storeId}")
    fun getStoreDetail(
        @AuthenticationPrincipal userId: Long,
        @PathVariable storeId: Long,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
    ): StoreDetailResponse = storeService.getStoreDetail(storeId, lat, lng, userId)

    @PatchMapping("/{storeId}")
    @PreAuthorize("hasRole('OWNER')")
    fun updateStore(
        @AuthenticationPrincipal ownerUserId: Long,
        @PathVariable storeId: Long,
        @Valid @RequestBody request: StoreUpdateRequest,
    ): StoreDetailResponse = storeService.updateStore(ownerUserId, storeId, request)

    @PutMapping("/{storeId}/menus")
    @PreAuthorize("hasRole('OWNER')")
    fun replaceMenus(
        @AuthenticationPrincipal ownerUserId: Long,
        @PathVariable storeId: Long,
        @Valid @RequestBody request: MenuBulkUpdateRequest,
    ): MenuBulkUpdateResponse = storeService.replaceMenus(ownerUserId, storeId, request)

    @GetMapping("/search")
    @PreAuthorize("hasRole('OWNER')")
    fun searchStoresForPartnership(
        @AuthenticationPrincipal ownerUserId: Long,
        @RequestParam keyword: String,
        @RequestParam(required = false) category: StoreCategory?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): StoreSearchListResponse = storeService.searchStoresForPartnership(ownerUserId, keyword, category, page, size)
}
