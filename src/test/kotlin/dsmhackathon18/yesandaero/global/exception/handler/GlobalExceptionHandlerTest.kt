package dsmhackathon18.yesandaero.global.exception.handler

import dsmhackathon18.yesandaero.global.exception.EntityNotFoundException
import dsmhackathon18.yesandaero.global.exception.GlobalErrorCode
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    private fun request(method: String = "GET", uri: String = "/api/test") =
        MockHttpServletRequest(method, uri)

    @Test
    fun `커스텀 BusinessException은 해당 ErrorCode의 상태와 코드로 응답한다`() {
        val response = handler.handleBusinessException(EntityNotFoundException(), request())

        assertEquals(GlobalErrorCode.NOT_FOUND.status, response.statusCode)
        assertEquals(GlobalErrorCode.NOT_FOUND.code, response.body?.code)
        assertEquals(GlobalErrorCode.NOT_FOUND.message, response.body?.message)
    }

    @Test
    fun `BusinessException의 커스텀 메시지는 응답 메시지로 그대로 노출된다`() {
        val response = handler.handleBusinessException(EntityNotFoundException("게시글을 찾을 수 없습니다."), request())

        assertEquals("게시글을 찾을 수 없습니다.", response.body?.message)
    }

    @Test
    fun `IllegalArgumentException 등 잘못된 요청은 INVALID_REQUEST로 변환된다`() {
        val response = handler.handleBadRequest(IllegalArgumentException("bad"), request())

        assertEquals(GlobalErrorCode.INVALID_REQUEST.status, response.statusCode)
        assertEquals(GlobalErrorCode.INVALID_REQUEST.code, response.body?.code)
    }

    @Test
    fun `예상치 못한 Exception은 INTERNAL_SERVER_ERROR로 변환되고 상세 메시지는 노출되지 않는다`() {
        val response = handler.handleUnexpected(RuntimeException("db connection refused"), request())

        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR.status, response.statusCode)
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR.code, response.body?.code)
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR.message, response.body?.message)
    }

    @Test
    fun `응답 body에는 요청 경로가 포함된다`() {
        val response = handler.handleBusinessException(EntityNotFoundException(), request(uri = "/api/posts/1"))

        assertEquals("/api/posts/1", response.body?.path)
    }
}
