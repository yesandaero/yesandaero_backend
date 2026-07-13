package dsmhackathon18.yesandaero.global.exception

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String,
) {

    companion object {

        fun of(status: HttpStatus, code: String, message: String, path: String): ErrorResponse =
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                code = code,
                message = message,
                path = path,
            )

        fun of(errorCode: ErrorCode, path: String): ErrorResponse =
            of(errorCode.status, errorCode.code, errorCode.message, path)
    }
}
