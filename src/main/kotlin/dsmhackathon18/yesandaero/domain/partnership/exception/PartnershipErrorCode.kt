package dsmhackathon18.yesandaero.domain.partnership.exception

import dsmhackathon18.yesandaero.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class PartnershipErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {

    // 403
    NOT_PARTNERSHIP_PARTY(HttpStatus.FORBIDDEN, "PTN_403", "해당 제휴의 당사자가 아닙니다"),

    // 404
    PARTNERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "PTN_404", "존재하지 않는 제휴입니다"),

    // 409
    PARTNERSHIP_ALREADY_EXISTS(HttpStatus.CONFLICT, "PTN_409_01", "이미 진행 중이거나 체결된 제휴입니다"),
    INVALID_PARTNERSHIP_STATUS(HttpStatus.CONFLICT, "PTN_409_02", "현재 상태에서 처리할 수 없는 요청입니다"),
}
