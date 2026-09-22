package com.example.urlshortener

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.URISyntaxException
import java.security.SecureRandom

@Service
class ShortLinkService(private val repository: ShortLinkRepository) {
    private val random = SecureRandom()
    private val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun create(targetUrl: String?): ShortLink {
        val target = targetUrl?.trim().orEmpty()
        require(target.isNotEmpty()) { "Enter a target URL." }
        require(target.length <= 2048) { "The target URL is too long." }

        val uri = try {
            URI(target)
        } catch (_: URISyntaxException) {
            throw IllegalArgumentException("Enter a valid http or https URL.")
        }
        require(uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "Enter a valid http or https URL."
        }

        repeat(5) {
            val slug = (1..7).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
            if (slug == "healthz") return@repeat
            try {
                // Each repository call has its own transaction, so a collision can be retried.
                return repository.saveAndFlush(ShortLink(slug = slug, targetUrl = target))
            } catch (_: DataIntegrityViolationException) {
                // The unique slug constraint handles simultaneous requests safely.
            }
        }
        throw IllegalStateException("Could not generate a unique slug.")
    }

    fun findTarget(slug: String): String? = repository.findBySlug(slug)?.targetUrl

    @Transactional
    fun recordClickAndFindTarget(slug: String): String? {
        val target = repository.findBySlug(slug)?.targetUrl ?: return null
        repository.incrementClickCount(slug)
        return target
    }

    fun recentLinks(): List<ShortLink> = repository.findTop20ByOrderByCreatedAtDescIdDesc()
}
