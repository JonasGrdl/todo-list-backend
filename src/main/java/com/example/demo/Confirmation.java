package com.example.demo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
class Confirmation {
    @Id @Column(length = 64)
    String hash;
    @ManyToOne(optional = false)
    Account account;
    @Column(nullable = false)
    String purpose = "verify-email";
    Instant expiresAt;
    boolean consumed;
}
