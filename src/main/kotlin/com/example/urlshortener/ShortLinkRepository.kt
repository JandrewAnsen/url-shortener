package com.example.urlshortener

import org.springframework.data.jpa.repository.JpaRepository

interface ShortLinkRepository : JpaRepository<ShortLink, Long> {
    fun findBySlug(slug: String): ShortLink?

    fun findTop20ByOrderByCreatedAtDescIdDesc(): List<ShortLink>
}
