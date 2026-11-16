package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class Verification {
    private final Accounts accounts;
    private final Confirmations tokens;
    private final Mailer mailer;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    Verification(Accounts accounts, Confirmations tokens, Mailer mailer, Clock clock) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.mailer = mailer;
        this.clock = clock;
    }

    enum Delivery { ACCEPTED, LIMITED, FAILED }

    @Transactional
    public Delivery issue(UUID id) {
        Account a = accounts.lock(id).orElseThrow();
        if (a.verified) return Delivery.ACCEPTED;
        Instant now = clock.instant();
        if (a.lastAttempt != null && now.isBefore(a.lastAttempt.plusSeconds(60))) return Delivery.LIMITED;
        if (a.windowStart == null || !now.isBefore(a.windowStart.plusSeconds(3600))) {
            a.windowStart = now;
            a.attempts = 0;
        }
        if (a.attempts >= 5) return Delivery.LIMITED;
        a.lastAttempt = now;
        a.attempts++;
        tokens.revoke(id);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        if (!mailer.send(a.email, raw)) return Delivery.FAILED;
        Confirmation token = new Confirmation();
        token.hash = digest(raw);
        token.account = a;
        token.expiresAt = now.plus(Duration.ofHours(24));
        tokens.save(token);
        return Delivery.ACCEPTED;
    }

    @Transactional
    public boolean verify(String raw) {
        String hash = digest(raw);
        var owner = tokens.owner(hash);
        if (owner.isEmpty()) return false;
        Account a = accounts.lock(owner.get()).orElseThrow();
        var result = tokens.findById(hash);
        if (result.isEmpty()) return false;
        Confirmation token = result.get();
        if (token.consumed || !token.expiresAt.isAfter(clock.instant()) || !token.purpose.equals("verify-email")) return false;
        token.consumed = true;
        a.verified = true;
        return true;
    }

    private String digest(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
