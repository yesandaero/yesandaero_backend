package dsmhackathon18.yesandaero.global.util

import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

data class Distance(
    val distanceMeters: Long,
    val walkingMinutes: Int,
)

object GeoDistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val WALKING_METERS_PER_MINUTE = 80.0

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Long {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_METERS * c).roundToLong()
    }

    fun walkingMinutes(distanceMeters: Long): Int =
        ceil(distanceMeters / WALKING_METERS_PER_MINUTE).toInt()

    /**
     * 사용자 좌표가 없으면 null. 있으면 distanceMeters/walkingMinutes를 함께 계산한다.
     */
    fun calculate(userLat: Double?, userLng: Double?, targetLat: Double, targetLng: Double): Distance? {
        if (userLat == null || userLng == null) return null
        val meters = distanceMeters(userLat, userLng, targetLat, targetLng)
        return Distance(meters, walkingMinutes(meters))
    }
}
