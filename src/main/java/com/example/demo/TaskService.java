package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class TaskService {
    private final TaskRepository repository;
    public TaskService(TaskRepository repository) { this.repository = repository; }
    public List<TaskView> getAll() { return repository.findAll().stream().map(TaskView::of).toList(); }
    private Task find(long id) {
        if (id < 1) throw new IllegalArgumentException("Die ID muss positiv sein.");
        return repository.findById(id).orElseThrow(NoSuchElementException::new);
    }
    public TaskView save(Long id, TaskInput input) {
        Task task = id == null ? new Task() : find(id);
        input.apply(task);
        return TaskView.of(repository.saveAndFlush(task));
    }
    public void delete(long id) { repository.delete(find(id)); }
}
