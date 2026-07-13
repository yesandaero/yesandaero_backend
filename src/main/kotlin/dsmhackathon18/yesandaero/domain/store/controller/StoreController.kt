package dsmhackathon18.yesandaero.domain.store.controller

import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterResponse
import dsmhackathon18.yesandaero.domain.store.service.StoreService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
}
