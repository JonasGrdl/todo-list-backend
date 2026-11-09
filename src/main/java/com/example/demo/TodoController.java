package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://todo-list-frontend-165i.onrender.com"
})
public class TodoController {

    private final TaskService service;

    public TodoController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/todos")
    public Iterable<Task> getTodos() {
        return service.getAll();
    }

    @PostMapping("/todos")
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTodo(@RequestBody Task task) {
        return service.save(task);
    }
}