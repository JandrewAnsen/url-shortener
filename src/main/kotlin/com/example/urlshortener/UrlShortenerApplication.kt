package com.example.urlshortener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.Profiles

@SpringBootApplication
class UrlShortenerApplication

fun main(args: Array<String>) {
	runApplication<UrlShortenerApplication>(*args) {
		addInitializers(ApplicationContextInitializer<ConfigurableApplicationContext> { context ->
			if (context.environment.acceptsProfiles(Profiles.of("prod"))) {
				throw IllegalStateException("STARTUP_FAILURE_EXPERIMENT: intentional failure before database migrations")
			}
		})
	}
}
