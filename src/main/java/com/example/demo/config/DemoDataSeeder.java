package com.example.demo.config;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.demo.task.TaskPriority;
import com.example.demo.task.TaskRequest;
import com.example.demo.task.TaskRepository;
import com.example.demo.task.TaskService;
import com.example.demo.task.TaskStatus;

/**
 * Legt Beispielaufgaben an, aber nur
 * 1. wenn das Profil "demo" aktiv ist (SPRING_PROFILES_ACTIVE=demo) und
 * 2. wenn die Tabelle noch leer ist.
 * Im normalen Betrieb läuft diese Klasse nicht, und vorhandene Daten werden nie verändert.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final TaskRepository repository;
    private final TaskService service;

    public DemoDataSeeder(TaskRepository repository, TaskService service) {
        this.repository = repository;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("Demo-Daten übersprungen: Es gibt bereits Aufgaben.");
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
        add("Übungsblatt 4 Statistik abgeben", "Aufgaben 2 bis 5, als PDF hochladen.",
                TaskPriority.HIGH, today.plusDays(2), TaskStatus.OPEN);
        add("Vue-Komponenten für das WebTech-Projekt testen", "Vitest-Tests für Formular und Liste ergänzen.",
                TaskPriority.MEDIUM, today.plusDays(7), TaskStatus.OPEN);
        add("Datenbanken: Kapitel 3 wiederholen", "Normalformen und Joins zusammenfassen.",
                TaskPriority.MEDIUM, today.minusDays(1), TaskStatus.OPEN);
        add("Bibliotheksbuch zurückgeben", null, TaskPriority.LOW, today, TaskStatus.OPEN);
        add("Gruppentreffen vorbereiten", "Agenda für Donnerstag schicken.",
                TaskPriority.LOW, null, TaskStatus.OPEN);
        add("Controlling-Klausur anmelden", null, TaskPriority.HIGH, today.minusDays(3), TaskStatus.DONE);
        log.info("Demo-Daten angelegt.");
    }

    private void add(String title, String description, TaskPriority priority, LocalDate due, TaskStatus status) {
        service.create(new TaskRequest(title, description, priority, due, status));
    }
}
