package dsmhackathon18.yesandaero.domain.store.exception

import dsmhackathon18.yesandaero.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class StoreErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {

    // 403
    NOT_STORE_OWNER(HttpStatus.FORBIDDEN, "STR_403", "본인 소유의 가게가 아닙니다"),

    // 404
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STR_404", "존재하지 않는 가게입니다"),

    // 409
    STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "STR_409", "이미 등록된 가게가 있습니다"),
}
