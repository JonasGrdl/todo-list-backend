package com.example.demo;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Aktualisiert einen bestehenden Task (name, description, done).
     * Ergebnis: Optional mit dem gespeicherten Task, oder leer, wenn die id unbekannt ist.
     * Entscheidung: Laden, Ändern und Speichern laufen in einer Transaktion (@Transactional),
     * und die id kommt nur aus dem Pfad. Die id aus "changes" wird nie benutzt,
     * so kann ein Client keinen fremden Task überschreiben.
     * Die Felder werden vollständig ersetzt (description darf null werden).
     */
    @Transactional
    public Optional<Task> update(Long id, Task changes) {
        return repository.findById(id).map(existing -> {
            existing.setName(changes.getName());
            existing.setDescription(changes.getDescription());
            existing.setDone(changes.isDone());
            return repository.save(existing);
        });
    }
}
