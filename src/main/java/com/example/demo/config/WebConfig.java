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
 * FRONTEND_URL (kommagetrennt). Fremde Origins erhalten beim Preflight 403.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        // Leerzeichen und einen Schrägstrich am Ende entfernen: Der Browser schickt den Origin
        // immer ohne "/", z. B. "https://app.onrender.com".
        this.allowedOrigins = allowedOrigins.stream()
                .map(String::strip)
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .filter(s -> !s.isEmpty())
                .toList();
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
