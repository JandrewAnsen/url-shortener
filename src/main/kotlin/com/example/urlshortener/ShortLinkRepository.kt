package com.example.urlshortener

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ShortLinkRepository : JpaRepository<ShortLink, Long> {
    fun findBySlug(slug: String): ShortLink?

    fun findTop20ByOrderByCreatedAtDescIdDesc(): List<ShortLink>

    @Modifying
    @Query("update ShortLink link set link.clickCount = link.clickCount + 1 where link.slug = :slug")
    fun incrementClickCount(@Param("slug") slug: String): Int
}
