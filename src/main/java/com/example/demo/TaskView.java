package com.example.demo;

import java.time.*;
public record TaskView(Long id, String name, String description, boolean done, String status,
                       String priority, LocalDate dueDate, Instant createdAt, Instant updatedAt) {
    static TaskView of(Task t) {
        return new TaskView(t.id, t.name, t.description, t.done, t.done ? "erledigt" : "offen",
                t.priority, t.dueDate, t.createdAt, t.updatedAt);
    }
}
