package com.example.demo.task;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Eingabe für POST /todos und PUT /todos/{id}.
 * Felder wie id, createdAt und updatedAt gibt es hier absichtlich nicht:
 * Schickt ein Client sie trotzdem mit, werden sie ignoriert.
 *
 * @param title       Pflichtfeld, wird getrimmt, danach 1-120 Zeichen.
 *                    "name" wird als alter Feldname weiterhin akzeptiert.
 * @param description optional, höchstens 2000 Zeichen; leerer Text wird zu null
 * @param priority    optional, Standard MEDIUM
 * @param dueDate     optional, Format JJJJ-MM-TT; vergangene Termine sind erlaubt
 * @param status      optional; beim Erstellen Standard OPEN, beim Bearbeiten bleibt
 *                    ohne Angabe der bisherige Status erhalten
 */
public record TaskRequest(
        @JsonAlias("name")
        @NotBlank(message = "Bitte gib einen Titel ein.")
        @Size(max = Task.TITLE_MAX, message = "Der Titel darf höchstens {max} Zeichen lang sein.")
        String title,

        @Size(max = Task.DESCRIPTION_MAX, message = "Die Beschreibung darf höchstens {max} Zeichen lang sein.")
        String description,

        TaskPriority priority,

        LocalDate dueDate,

        TaskStatus status) {

    /** Trimmt den Titel vor der Prüfung, damit "  Lernen  " gültig und "   " ungültig ist. */
    public TaskRequest {
        title = title == null ? null : title.strip();
        description = description == null || description.isBlank() ? null : description;
    }
}
