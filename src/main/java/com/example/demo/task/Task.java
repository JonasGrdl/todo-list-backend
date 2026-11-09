package com.example.demo.task;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Eine Aufgabe, wie sie in der Tabelle "task" gespeichert ist.
 * Die Entity wird nie direkt als JSON verschickt: Nach außen gehen nur
 * TaskRequest (Eingabe) und TaskResponse (Ausgabe). So kann ein Client
 * weder die id noch die Zeitstempel setzen.
 * Die Spaltengrenzen entsprechen der Flyway-Migration V2 und den Prüfungen in TaskRequest.
 */
@Entity
@Table(name = "task")
public class Task {

    public static final int TITLE_MAX = 120;
    public static final int DESCRIPTION_MAX = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TITLE_MAX)
    private String title;

    @Column(length = DESCRIPTION_MAX)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskStatus status = TaskStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;

    // Reines Kalenderdatum ohne Uhrzeit (Spaltentyp date)
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
        // für JPA
    }

    public Task(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Wird nur vom TaskService aufgerufen, der die Uhrzeit vorgibt. */
    void markCreated(Instant now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Wird nur vom TaskService aufgerufen, der die Uhrzeit vorgibt. */
    void markUpdated(Instant now) {
        this.updatedAt = now;
    }
}
