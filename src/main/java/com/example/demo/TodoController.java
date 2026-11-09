package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/todos")
@CrossOrigin(origins = {"http://localhost:5173", "${FRONTEND_URL:https://todo-list-frontend-165i.onrender.com}"})
public class TodoController {
    private final TaskService service;
    public TodoController(TaskService service) { this.service = service; }
    @GetMapping public List<TaskView> getTodos() { return service.getAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TaskView create(@RequestBody TaskInput task) { return service.save(null, task); }
    @PutMapping("/{id}")
    public TaskView update(@PathVariable long id, @RequestBody TaskInput task) { return service.save(id, task); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
