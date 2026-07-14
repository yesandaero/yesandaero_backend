package dsmhackathon18.yesandaero.domain.store.repository

import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StoreRepository : JpaRepository<Store, Long> {

    fun existsByOwnerUserId(ownerUserId: Long): Boolean

    fun findByOwnerUserId(ownerUserId: Long): Store?

    @Query(
        """
        SELECT s FROM Store s
        WHERE s.category IN :categories
        AND (:maxPrice IS NULL OR s.avgPrice <= :maxPrice)
        """,
    )
    fun findAllByFilters(
        @Param("categories") categories: List<StoreCategory>,
        @Param("maxPrice") maxPrice: Int?,
        pageable: Pageable,
    ): Page<Store>

    @Query(
        """
        SELECT s FROM Store s
        WHERE s.category IN :categories
        AND (:maxPrice IS NULL OR s.avgPrice <= :maxPrice)
        """,
    )
    fun findAllByFilters(
        @Param("categories") categories: List<StoreCategory>,
        @Param("maxPrice") maxPrice: Int?,
    ): List<Store>

    @Query(
        value = """
            SELECT s FROM Store s LEFT JOIN Menu m ON m.store = s
            WHERE s.category IN :categories
            AND (:maxPrice IS NULL OR s.avgPrice <= :maxPrice)
            GROUP BY s
            ORDER BY MAX(
                CASE WHEN m.discountedPrice IS NOT NULL AND m.price > 0
                    THEN (CAST(m.price AS double) - m.discountedPrice) / m.price
                    ELSE 0.0
                END
            ) DESC
        """,
        countQuery = """
            SELECT COUNT(s) FROM Store s
            WHERE s.category IN :categories
            AND (:maxPrice IS NULL OR s.avgPrice <= :maxPrice)
        """,
    )
    fun findAllByFiltersOrderByDiscountDesc(
        @Param("categories") categories: List<StoreCategory>,
        @Param("maxPrice") maxPrice: Int?,
        pageable: Pageable,
    ): Page<Store>

    @Query(
        """
        SELECT s FROM Store s
        WHERE s.latitude BETWEEN :swLat AND :neLat
        AND s.longitude BETWEEN :swLng AND :neLng
        AND s.category IN :categories
        AND (:maxPrice IS NULL OR s.avgPrice <= :maxPrice)
        ORDER BY s.createdAt DESC
        """,
    )
    fun findAllInBoundingBox(
        @Param("swLat") swLat: Double,
        @Param("swLng") swLng: Double,
        @Param("neLat") neLat: Double,
        @Param("neLng") neLng: Double,
        @Param("categories") categories: List<StoreCategory>,
        @Param("maxPrice") maxPrice: Int?,
    ): List<Store>

    @Query(
        """
        SELECT s FROM Store s
        WHERE s.ownerUserId <> :ownerUserId
        AND LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        AND (:category IS NULL OR s.category = :category)
        """,
    )
    fun searchForPartnership(
        @Param("ownerUserId") ownerUserId: Long,
        @Param("keyword") keyword: String,
        @Param("category") category: StoreCategory?,
        pageable: Pageable,
    ): Page<Store>
}
