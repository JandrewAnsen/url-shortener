package com.example.urlshortener

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestVersionFilter(private val version: DeploymentVersion) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        response.setHeader("X-App-Version", version.full)
        try {
            chain.doFilter(request, response)
        } finally {
            if (request.requestURI != "/healthz") {
                log.info("Request {} {} -> {} build={}", request.method, request.requestURI, response.status, version.full)
            }
        }
    }
}
