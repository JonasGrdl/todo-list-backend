package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TodoController {

    @GetMapping("/todos")
    public Task getTodos() {
        Task ersterTask = new Task("Erster Task");
        ersterTask.setDescription("Das ist ein Task");
        return ersterTask;
    }
}