package dsmhackathon18.yesandaero.global.exception

// 서비스 전반에서 사용하는 커스텀 예외의 베이스 클래스
open class BusinessException(
    val errorCode: ErrorCode,
    message: String = errorCode.message,
) : RuntimeException(message)
