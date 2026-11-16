package com.example.demo;

import java.time.*;
import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
class AuthController {
    record Register(@NotBlank @Email @Size(max=254) String email, String displayName, String password) {
        Register { email = normalize(email); }
    }
    record Profile(String displayName) {}
    record Credentials(@NotBlank @Email @Size(max=254) String email, String password) {
        Credentials { email = normalize(email); }
    }
    record Address(@NotBlank @Email @Size(max=254) String email) {
        Address { email = normalize(email); }
    }
    record Proof(@NotNull @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) {}
    record SessionInfo(String state, String displayName) {}
    record Csrf(String token) {}
    private final Accounts accounts;
    private final Verification verification;
    private final Limits limits;
    private final Argon2PasswordEncoder passwords;
    private final HttpSessionSecurityContextRepository contexts;
    private final Clock clock;
    private final String dummyHash;

    AuthController(Accounts accounts, Verification verification, Limits limits, Argon2PasswordEncoder passwords,
                   HttpSessionSecurityContextRepository contexts, Clock clock) {
        this.accounts = accounts;
        this.verification = verification;
        this.limits = limits;
        this.passwords = passwords;
        this.contexts = contexts;
        this.clock = clock;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }
    private static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
    private static boolean length(String s, int min, int max) {
        if (s == null) return false;
        int n = s.codePointCount(0, s.length());
        return n >= min && n <= max;
    }

    private static String displayName(String input) {
        String name = input == null ? "" : input.replaceAll("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$", "");
        if (!length(name, 2, 50)) throw new ApiError(400, "Der Anzeigename muss 2–50 Zeichen lang sein.");
        return name;
    }

    @PutMapping("/profile")
    @org.springframework.transaction.annotation.Transactional
    public SessionInfo profile(@RequestBody Profile input, Authentication auth) {
        Account a = accounts.lock((UUID) auth.getPrincipal()).orElseThrow();
        a.displayName = displayName(input.displayName());
        return new SessionInfo("authenticated", a.displayName);
    }

    @GetMapping("/csrf") Csrf csrf(CsrfToken token) { return new Csrf(token.getToken()); }

    @PostMapping("/register") @ResponseStatus(HttpStatus.ACCEPTED)
    void register(@Valid @RequestBody Register input, HttpServletRequest req) {
        limits.check("mail:" + req.getRemoteAddr(), 20, 3600);
        String name = displayName(input.displayName());
        if (!length(input.password(), 15, 128))
            throw new ApiError(400, "Das Passwort muss 15–128 Zeichen lang sein.");
        Account a = new Account();
        a.email = input.email();
        a.displayName = name;
        a.passwordHash = passwords.encode(input.password());
        if (accounts.findByEmail(a.email).isPresent()) return;
        try {
            a = accounts.saveAndFlush(a);
        } catch (DataIntegrityViolationException ex) {
            if (accounts.findByEmail(a.email).isPresent()) return;
            throw ex;
        }
        delivered(verification.issue(a.id));
    }

    @PostMapping("/resend") @ResponseStatus(HttpStatus.ACCEPTED)
    void resend(@Valid @RequestBody Address input, HttpServletRequest req) {
        limits.check("mail:" + req.getRemoteAddr(), 20, 3600);
        limits.check("resend:" + input.email(), 1, 60);
        accounts.findByEmail(input.email()).ifPresent(a -> delivered(verification.issue(a.id)));
    }

    private void delivered(Verification.Delivery result) {
        if (result == Verification.Delivery.LIMITED) throw ApiError.limited();
        if (result == Verification.Delivery.FAILED)
            throw new ApiError(503, "Versand fehlgeschlagen. Bitte fordere nach einer Minute einen neuen Link an.");
    }

    @PostMapping("/verify") @ResponseStatus(HttpStatus.NO_CONTENT)
    void verify(@Valid @RequestBody Proof input) {
        if (!verification.verify(input.token()))
            throw new ApiError(400, "Der Link ist ungültig, abgelaufen oder bereits verwendet. Fordere bei Bedarf einen neuen an.");
    }

    @PostMapping("/login")
    SessionInfo login(@Valid @RequestBody Credentials input, HttpServletRequest req, HttpServletResponse res) {
        limits.check("login:" + req.getRemoteAddr(), 40, 600);
        limits.check("login:" + req.getRemoteAddr() + ":" + input.email(), 8, 600);
        limits.check("account-login:" + input.email(), 80, 600);
        Account a = accounts.findByEmail(input.email()).orElse(null);
        if (!length(input.password(), 15, 128) || !passwords.matches(input.password(), a == null ? dummyHash : a.passwordHash) || a == null)
            throw new ApiError(401, "E-Mail-Adresse oder Passwort ist falsch.");
        var old = req.getSession(false);
        if (old != null) old.invalidate();
        if (!a.verified) return new SessionInfo("pending", null);
        var session = req.getSession(true);
        Instant now = clock.instant();
        session.setAttribute("loginAt", now);
        session.setAttribute("activeAt", now);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(a.id, null,
                List.of(new SimpleGrantedAuthority("ROLE_VERIFIED"))));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, req, res);
        return new SessionInfo("authenticated", a.displayName);
    }

    @GetMapping("/session")
    SessionInfo session(Authentication auth) {
        if (auth == null) return new SessionInfo("anonymous", null);
        return accounts.findById((UUID) auth.getPrincipal()).map(a -> new SessionInfo("authenticated", a.displayName))
                .orElse(new SessionInfo("anonymous", null));
    }
}
