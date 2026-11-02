package com.example.demo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class TodoController {

    private List<Task> tasks = new ArrayList<>();

    public TodoController() {
        tasks.add(new Task("Erster Task"));
        tasks.get(0).setDescription("Das ist ein Task");
    }

    @GetMapping("/todos")
    public List<Task> getTodos() {
        return tasks;
    }
}
