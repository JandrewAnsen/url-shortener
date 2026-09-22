package com.example.urlshortener

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Controller
class LinkController(private val links: ShortLinkService, private val version: DeploymentVersion) {
    @GetMapping("/")
    fun index(@RequestParam(required = false) created: String?, model: Model): String {
        if (created != null && links.findTarget(created) != null) {
            val shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .pathSegment(created)
                .toUriString()
            model.addAttribute("shortUrl", shortUrl)
        }
        model.addAttribute("links", links.recentLinks())
        model.addAttribute("appVersion", version.short)
        return "index"
    }

    @PostMapping("/links")
    fun create(
        @RequestParam(required = false) targetUrl: String?,
        model: Model,
        redirectAttributes: RedirectAttributes,
    ): String {
        val link = try {
            links.create(targetUrl)
        } catch (exception: IllegalArgumentException) {
            model.addAttribute("error", exception.message)
            model.addAttribute("targetUrl", targetUrl)
            return index(null, model)
        }

        redirectAttributes.addAttribute("created", link.slug)
        return "redirect:/"
    }

    @GetMapping("/healthz")
    fun health(): ResponseEntity<String> = ResponseEntity.ok("OK")

    @GetMapping("/{slug:[a-zA-Z0-9]{7}}")
    fun redirect(@PathVariable slug: String): ResponseEntity<Void> {
        val target = links.recordClickAndFindTarget(slug) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, target)
            .build()
    }
}
