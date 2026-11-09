package com.example.demo;

import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Service-Schicht: enthält die Logik rund um Tasks und spricht mit dem
 * Repository (der Datenbank-Schicht). Der Controller ruft nur diese Klasse auf.
 */
@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /** Speichert einen neuen oder geänderten Task und gibt ihn mit id zurück. */
    public Task save(Task task) {
        return repository.save(task);
    }

    /** Gibt alle gespeicherten Tasks zurück. */
    public Iterable<Task> getAll() {
        return repository.findAll();
    }

    /**
     * Sucht einen Task anhand seiner id.
     * Ergebnis: Optional mit dem Task, oder leer, wenn es keinen Task mit dieser id gibt.
     */
    public Optional<Task> getById(Long id) {
        // findById liefert ein Optional, damit "nicht gefunden" ohne null auskommt
        return repository.findById(id);
    }
}