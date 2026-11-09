package com.example.demo;

import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public Task save(Task task) {
        return repository.save(task);
    }

    public Iterable<Task> getAll() {
        return repository.findAll();
    }
}