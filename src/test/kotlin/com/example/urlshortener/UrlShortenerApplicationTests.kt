package com.example.urlshortener

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(properties = ["RENDER_GIT_COMMIT=abc123456789"])
@AutoConfigureMockMvc
class UrlShortenerApplicationTests {
    @Autowired
    lateinit var links: ShortLinkService

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var repository: ShortLinkRepository

    @Test
    fun `http and https targets can be shortened and found`() {
        for (target in listOf("http://example.com/a", "https://example.com/b")) {
            val link = links.create(target)
            assertTrue(link.slug.matches(Regex("[A-Za-z0-9]{7}")))
            assertNotNull(link.id)
            assertEquals(target, links.findTarget(link.slug))
        }
    }

    @Test
    fun `invalid targets are rejected`() {
        for (target in listOf(null, "  ", "ftp://example.com", "https://", "not a URL")) {
            assertFailsWith<IllegalArgumentException> { links.create(target) }
        }
    }

    @Test
    fun `form creates a redirecting link`() {
        mvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Shorten")))

        val result = mvc.perform(post("/links").param("targetUrl", "https://example.com/path"))
            .andExpect(status().is3xxRedirection)
            .andReturn()
        val redirect = result.response.redirectedUrl!!
        assertTrue(redirect.startsWith("/?created="))
        val slug = redirect.substringAfter("created=")
        assertTrue(slug.matches(Regex("[A-Za-z0-9]{7}")))

        mvc.perform(get(redirect))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost/$slug")))

        mvc.perform(get("/$slug"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "https://example.com/path"))
    }

    @Test
    fun `page and responses identify the running commit`() {
        mvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(header().string("X-App-Version", "abc123456789"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Running build <code>abc1234</code>")))

        mvc.perform(get("/healthz"))
            .andExpect(header().string("X-App-Version", "abc123456789"))
    }

    @Test
    fun `unknown slug returns 404`() {
        assertNull(links.findTarget("0000000"))
        mvc.perform(get("/0000000")).andExpect(status().isNotFound)
    }

    @Test
    fun `successful redirects count clicks without counting page views`() {
        val link = links.create("https://example.com/count-me")

        mvc.perform(get("/?created=${link.slug}")).andExpect(status().isOk)
        assertEquals(0, repository.findBySlug(link.slug)?.clickCount)

        repeat(2) {
            mvc.perform(get("/${link.slug}"))
                .andExpect(status().isFound)
                .andExpect(header().string("Location", "https://example.com/count-me"))
        }

        assertEquals(2, repository.findBySlug(link.slug)?.clickCount)
        mvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/${link.slug}\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Clicks: 2")))
    }

    @Test
    fun `health check returns OK`() {
        mvc.perform(get("/healthz"))
            .andExpect(status().isOk)
            .andExpect(content().string("OK"))
    }
}
