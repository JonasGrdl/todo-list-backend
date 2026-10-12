package com.example.demo;

public class Task {

    // Attribute
    private String name;
    private String description;
    private boolean done;


    // Constructor
    public Task(String name) {
        this.name = name;
    }

    // Getter
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return done;
    }

    // Setter
    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
