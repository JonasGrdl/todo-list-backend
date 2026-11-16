package com.example.demo;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", updatable = false)
    Account owner;
    String name;
    @Column(length = 2000)
    String description;
    boolean done;
    String priority;
    LocalDate dueDate;
    Instant createdAt;
    Instant updatedAt;
}
