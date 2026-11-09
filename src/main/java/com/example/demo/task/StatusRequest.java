package com.example.demo.task;

import jakarta.validation.constraints.NotNull;

/** Eingabe für PATCH /todos/{id}/status, z. B. {"status": "DONE"}. */
public record StatusRequest(
        @NotNull(message = "Bitte gib einen Status an (OPEN oder DONE).")
        TaskStatus status) {
}
