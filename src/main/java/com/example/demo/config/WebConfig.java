package com.example.demo.config;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Zentrale Web-Konfiguration.
 * CORS: Erlaubt sind nur die Frontend-Adressen aus der Umgebungsvariable
 * CORS_ALLOWED_ORIGINS (kommagetrennt). Fremde Origins erhalten beim Preflight 403.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/todos/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("Content-Type")
                .exposedHeaders("Location")
                .maxAge(3600);
    }

    /** Systemuhr für Zeitstempel; Tests ersetzen sie durch eine feste Uhr. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
