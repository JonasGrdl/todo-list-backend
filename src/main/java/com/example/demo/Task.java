package com.example.demo;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    @Column(length = 2000)
    String description;
    boolean done;
    String priority;
    LocalDate dueDate;
    Instant createdAt;
    Instant updatedAt;

    @PrePersist void created() { createdAt = updatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
    @PreUpdate void updated() { updatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
}
