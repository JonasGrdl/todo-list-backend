package com.example.demo.task;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Geschäftslogik für Aufgaben. Der Controller kennt nur diese Klasse,
 * das Repository wird nur hier benutzt.
 * Die Zeitstempel setzt ausschließlich der Service. Die Uhr (Clock) wird
 * hereingereicht, damit Tests eine feste Uhrzeit vorgeben können.
 */
@Service
public class TaskService {

    // Neueste zuerst; die id entscheidet bei gleichem Zeitpunkt (stabile Reihenfolge)
    private static final Sort DEFAULT_ORDER = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TaskRepository repository;
    private final Clock clock;

    public TaskService(TaskRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return repository.findAll(DEFAULT_ORDER).stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return TaskResponse.from(load(id));
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task(request.title());
        applyFields(task, request);
        task.setStatus(request.status() != null ? request.status() : TaskStatus.OPEN);
        task.markCreated(now());
        return TaskResponse.from(repository.save(task));
    }

    /** Ersetzt Titel, Beschreibung, Priorität und Termin. Status nur, wenn mitgeschickt. */
    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = load(id);
        applyFields(task, request);
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        task.markUpdated(now());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long id, TaskStatus status) {
        Task task = load(id);
        task.setStatus(status);
        task.markUpdated(now());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(load(id));
    }

    private Task load(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private static void applyFields(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setDueDate(request.dueDate());
    }

    // PostgreSQL speichert Mikrosekunden. Ohne Abschneiden wäre der zurückgegebene
    // Zeitstempel genauer als der gespeicherte und ein Neuladen lieferte einen anderen Wert.
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
