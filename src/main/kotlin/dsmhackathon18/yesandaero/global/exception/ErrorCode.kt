package dsmhackathon18.yesandaero.global.exception

import org.springframework.http.HttpStatus

interface ErrorCode {

    val status: HttpStatus

    val code: String

    val message: String
}
