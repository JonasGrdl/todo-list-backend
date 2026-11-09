package com.example.demo.task;

/** Wird geworfen, wenn es zu einer id keine Aufgabe gibt. Wird zu HTTP 404. */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Aufgabe mit der ID " + id + " wurde nicht gefunden.");
    }
}
