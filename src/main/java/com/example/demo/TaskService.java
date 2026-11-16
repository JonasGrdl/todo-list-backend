package com.example.demo;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.*;

@Service
@Transactional
class TaskService {
    record Draft(@NotBlank @Size(max=120) String name, @Size(max=2000) String description,
                 @Pattern(regexp="niedrig|mittel|hoch") String priority,
                 @Pattern(regexp="offen|erledigt") String status, LocalDate dueDate, Boolean done) {}
    record View(long id, String name, String description, String priority, String status, boolean done,
                LocalDate dueDate, Instant createdAt, Instant updatedAt) {
        View(Task t) {
            this(t.id, t.name, t.description, t.priority, t.done ? "erledigt" : "offen", t.done,
                    t.dueDate, t.createdAt, t.updatedAt);
        }
    }
    private final TaskRepository tasks;
    private final Accounts accounts;
    private final Clock clock;
    TaskService(TaskRepository tasks, Accounts accounts, Clock clock) {
        this.tasks = tasks;
        this.accounts = accounts;
        this.clock = clock;
    }
    private Task owned(UUID owner, long id) {
        return tasks.findByIdAndOwner_Id(id, owner).orElseThrow(() -> new ApiError(404, "Aufgabe nicht gefunden."));
    }
    public List<View> list(UUID owner) { return tasks.findByOwner_Id(owner).stream().map(View::new).toList(); }
    public View get(UUID owner, long id) { return new View(owned(owner, id)); }
    public View save(UUID owner, Long id, Draft d) {
        Task t = id == null ? new Task() : owned(owner, id);
        t.updatedAt = clock.instant();
        if (id == null) {
            t.owner = accounts.getReferenceById(owner);
            t.createdAt = t.updatedAt;
        }
        t.name = d.name().strip();
        t.description = d.description() == null ? "" : d.description();
        t.priority = d.priority() == null ? "mittel" : d.priority();
        t.done = d.status() == null ? Boolean.TRUE.equals(d.done()) : "erledigt".equals(d.status());
        t.dueDate = d.dueDate();
        return new View(tasks.save(t));
    }
    public void delete(UUID owner, long id) { tasks.delete(owned(owner, id)); }
}
