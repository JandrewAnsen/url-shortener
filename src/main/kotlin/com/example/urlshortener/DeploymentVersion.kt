package com.example.urlshortener

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DeploymentVersion(@Value("\${RENDER_GIT_COMMIT:local}") commit: String) {
    val full = commit.ifBlank { "local" }
    val short = full.take(7)
}
