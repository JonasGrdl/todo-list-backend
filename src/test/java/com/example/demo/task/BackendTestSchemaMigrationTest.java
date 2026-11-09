package com.example.demo.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Nachstellung des fehlgeschlagenen Render-Deployments vom 09.11.2026:
 * Die Datenbank wurde vorher vom Branch "backend-test" (schema.sql) umgebaut
 * (Spalte priority mit deutschen Werten, Zeitstempel, Constraint task_priority_valid).
 * Der Test startet die komplette Anwendung dagegen – wie Render: Flyway migriert,
 * danach prüft Hibernate das Schema (ddl-auto=validate). Alle Daten müssen erhalten bleiben.
 */
@SpringBootTest
@Testcontainers
class BackendTestSchemaMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("db/backend-test-schema.sql");

    @Autowired
    TaskRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migratesBackendTestSchemaAndKeepsAllData() {
        // Nur die drei Altdatensätze (der andere Test fügt ggf. eine Zeile hinzu)
        List<Task> tasks = repository.findAll(Sort.by("id")).stream().filter(t -> t.getId() <= 3).toList();
        assertThat(tasks).hasSize(3);

        Task first = tasks.get(0);
        assertThat(first.getTitle()).isEqualTo("Web-Tech Abgabe fertig machen");
        assertThat(first.getDescription()).isEqualTo("Milestone 5");
        assertThat(first.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(first.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(first.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(first.getCreatedAt()).isEqualTo(Instant.parse("2026-11-01T10:00:00Z"));
        assertThat(first.getUpdatedAt()).isEqualTo(Instant.parse("2026-11-02T10:00:00Z"));

        Task second = tasks.get(1);
        assertThat(second.getTitle()).isEqualTo("Lernen");
        assertThat(second.getDescription()).isNull(); // leerer Text -> keine Beschreibung
        assertThat(second.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(second.getPriority()).isEqualTo(TaskPriority.MEDIUM);

        assertThat(tasks.get(2).getPriority()).isEqualTo(TaskPriority.LOW);
    }

    @Test
    void newConstraintsAcceptNewValuesAndIdsContinue() {
        Long id = jdbc.queryForObject("""
                INSERT INTO task (title, status, priority, created_at, updated_at)
                VALUES ('Neu', 'OPEN', 'HIGH', now(), now()) RETURNING id""", Long.class);
        assertThat(id).isGreaterThan(3L);

        Integer oldColumns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name = 'task' AND column_name IN ('name', 'done')""", Integer.class);
        assertThat(oldColumns).isZero();
    }
}
