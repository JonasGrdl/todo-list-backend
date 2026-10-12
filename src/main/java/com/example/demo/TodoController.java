package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TodoController {

    private List<Task> tasks = new ArrayList<>();

    @GetMapping("/todos")
    public List<Task> getTodos() {
        tasks.add(new Task("Erster Task"));
        tasks.get(0).setDescription("Das ist ein Task");
        return tasks;
    }
}