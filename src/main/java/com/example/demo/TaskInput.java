package com.example.demo;

import java.time.LocalDate;
import java.util.Set;

public record TaskInput(String name, String description, String status, String done,
                        String priority, LocalDate dueDate) {
    void apply(Task task) {
        String title = name == null ? "" : name.strip();
        if (title.isEmpty() || title.length() > 120)
            throw new IllegalArgumentException("Der Titel muss 1 bis 120 Zeichen enthalten.");
        if (description != null && description.length() > 2000)
            throw new IllegalArgumentException("Die Beschreibung darf höchstens 2000 Zeichen enthalten.");
        if (status != null && !Set.of("offen", "erledigt").contains(status))
            throw new IllegalArgumentException("Ungültiger Status.");
        if (done != null && !Set.of("true", "false").contains(done))
            throw new IllegalArgumentException("Ungültiger Erledigt-Wert.");
        if (status != null && done != null && status.equals("erledigt") != Boolean.parseBoolean(done))
            throw new IllegalArgumentException("Widersprüchlicher Status.");
        if (priority != null && !Set.of("niedrig", "mittel", "hoch").contains(priority))
            throw new IllegalArgumentException("Ungültige Priorität.");
        task.name = title;
        task.description = description == null ? "" : description;
        task.done = status == null ? Boolean.parseBoolean(done) : status.equals("erledigt");
        task.priority = priority == null ? "mittel" : priority;
        task.dueDate = dueDate;
    }
}
