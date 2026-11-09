package com.example.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Testinfrastruktur: Für jeden Testlauf startet Testcontainers eine eigene, leere
 * PostgreSQL-Instanz in Docker. @ServiceConnection leitet die Verbindungsdaten
 * automatisch an Spring weiter. Die echte (lokale oder produktive) Datenbank
 * wird nie berührt.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-11-09T08:00:00Z"));
    }

    /** Uhr, die Tests gezielt vorstellen können, um Zeitstempel exakt zu prüfen. */
    public static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        public void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
