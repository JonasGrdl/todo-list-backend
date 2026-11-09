package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Ein Task (eine Aufgabe in der Todo-Liste). Wird als Datensatz in der Datenbank
 * gespeichert (@Entity) und als JSON an das Frontend geschickt.
 */
@Entity
public class Task {

    // Attribute
    private String name;
    private String description;
    private boolean done;

    // ID generator
    // Die id wird von der Datenbank vergeben. READ_ONLY heißt: Sie wird im JSON
    // ausgegeben, aber beim Empfangen eines Requests ignoriert.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    // Constructor
    public Task(String name) {
        this.name = name;
    }
    public Task() {}

    // Getter
    /** Gibt die von der Datenbank vergebene id zurück (null, solange der Task nicht gespeichert ist). */
    public Long getId() {
        return id;
    }

    /** Gibt den Namen des Tasks zurück. */
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return done;
    }

    // Setter
    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
