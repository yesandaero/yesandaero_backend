package dsmhackathon18.yesandaero.global.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GeoDistanceCalculatorTest {

    @Test
    fun `같은 좌표의 거리는 0m이다`() {
        val distance = GeoDistanceCalculator.distanceMeters(36.3624, 127.3568, 36.3624, 127.3568)

        assertEquals(0L, distance)
    }

    @Test
    fun `위도 1도 차이는 약 111195m이다`() {
        val distance = GeoDistanceCalculator.distanceMeters(0.0, 0.0, 1.0, 0.0)

        assertEquals(111195L, distance)
    }

    @Test
    fun `도보 시간은 80m 분 기준으로 올림 계산한다`() {
        assertEquals(2, GeoDistanceCalculator.walkingMinutes(160L))
        assertEquals(3, GeoDistanceCalculator.walkingMinutes(161L))
        assertEquals(0, GeoDistanceCalculator.walkingMinutes(0L))
    }

    @Test
    fun `사용자 좌표가 없으면 null을 반환한다`() {
        assertNull(GeoDistanceCalculator.calculate(null, null, 36.3624, 127.3568))
        assertNull(GeoDistanceCalculator.calculate(36.3624, null, 36.3624, 127.3568))
    }

    @Test
    fun `사용자 좌표가 있으면 거리와 도보시간을 함께 계산한다`() {
        val result = GeoDistanceCalculator.calculate(0.0, 0.0, 1.0, 0.0)

        assertEquals(111195L, result?.distanceMeters)
        assertEquals(1390, result?.walkingMinutes)
    }
}
