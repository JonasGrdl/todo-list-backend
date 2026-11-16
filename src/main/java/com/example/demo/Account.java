package com.example.demo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "todo_account")
class Account {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, unique = true, updatable = false, length = 254)
    String email;
    @Column(nullable = false)
    String displayName;
    @Column(nullable = false)
    String passwordHash;
    boolean verified;
    Instant lastAttempt;
    Instant windowStart;
    int attempts;
}
