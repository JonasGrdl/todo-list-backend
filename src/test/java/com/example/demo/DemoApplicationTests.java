package com.example.demo;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {
    @Autowired WebApplicationContext app;
    @Autowired ObjectMapper json;
    @Autowired Accounts accounts;
    @Autowired TaskRepository tasks;
    @MockitoBean Mailer mail;
    @MockitoBean Clock clock;
    MockMvc mvc;
    String email;
    String ip;
    final String password = "  mein 🔑 Passwort  ";
    final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    final AtomicReference<String> token = new AtomicReference<>();

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(app).apply(springSecurity()).build();
        email = UUID.randomUUID() + "+uni@example.org";
        ip = UUID.randomUUID().toString();
        when(clock.instant()).thenReturn(start);
        when(mail.send(anyString(), anyString())).thenAnswer(call -> {
            token.set(call.getArgument(1));
            return true;
        });
    }

    MockHttpServletRequestBuilder postJson(String path, Map<String, ?> fields) {
        return post(path).with(r -> { r.setRemoteAddr(ip); return r; }).with(csrf())
                .contentType("application/json").content(json.writeValueAsString(fields));
    }
    MockHttpServletRequestBuilder register(String address, String name, String pw) {
        return postJson("/auth/register", Map.of("email", address, "displayName", name, "password", pw));
    }
    MockHttpServletRequestBuilder proof(String raw) { return postJson("/auth/verify", Map.of("token", raw)); }
    MockHttpServletRequestBuilder login() {
        return postJson("/auth/login", Map.of("email", email, "password", password));
    }
    void enroll() throws Exception {
        mvc.perform(register(email, " Jonas ", password)).andExpect(status().isAccepted());
    }
    MockHttpSession signedIn() throws Exception {
        enroll();
        mvc.perform(proof(token.get())).andExpect(status().isNoContent());
        MockHttpSession old = new MockHttpSession();
        var result = mvc.perform(login().session(old)).andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("authenticated")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(old.isInvalid()).isTrue();
        assertThat(session.getId()).isNotEqualTo(old.getId());
        return session;
    }

    @Test void unicodeLengthsAndPasswordWhitespace() throws Exception {
        String longest = "🔑".repeat(126) + "  ";
        mvc.perform(register(email, "J", password)).andExpect(status().isBadRequest());
        mvc.perform(register(email, "J".repeat(51), password)).andExpect(status().isBadRequest());
        mvc.perform(register(email, "Jonas", "🔑".repeat(14))).andExpect(status().isBadRequest());
        mvc.perform(register(email, "Jonas", longest + " ")).andExpect(status().isBadRequest());
        mvc.perform(register(email, "🔑".repeat(50), longest)).andExpect(status().isAccepted());
        mvc.perform(postJson("/auth/login", Map.of("email", email, "password", longest.strip())))
                .andExpect(status().isUnauthorized());
        mvc.perform(postJson("/auth/login", Map.of("email", email, "password", longest)))
                .andExpect(jsonPath("$.state").value("pending"));
    }

    @Test void loginLimitExpires() throws Exception {
        enroll();
        for (int n = 0; n < 8; n++)
            mvc.perform(postJson("/auth/login", Map.of("email", email, "password", password + "X")))
                    .andExpect(status().isUnauthorized());
        mvc.perform(login()).andExpect(status().isTooManyRequests());
        when(clock.instant()).thenReturn(start.plusSeconds(600));
        mvc.perform(login()).andExpect(jsonPath("$.state").value("pending"));
    }

    @Test void duplicateCannotReplaceCredentialsOrProfile() throws Exception {
        enroll();
        mvc.perform(register(" " + email.toUpperCase(Locale.ROOT) + " ", "Andere Person", password + "X"))
                .andExpect(status().isAccepted());
        var a = accounts.findByEmail(email).orElseThrow();
        assertThat(a.displayName).isEqualTo("Jonas");
        assertThat(a.passwordHash).startsWith("$argon2id$");
        var pending = new MockHttpSession();
        mvc.perform(login().session(pending)).andExpect(jsonPath("$.state").value("pending"));
        assertThat(pending.isInvalid()).isTrue();
        mvc.perform(postJson("/todos", Map.of("name", "Versuch"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/todos")).andExpect(status().isUnauthorized());
        verify(mail, times(1)).send(eq(email), anyString());
    }
    int[] parallel(Callable<Integer> work) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> paired = () -> { barrier.await(5, TimeUnit.SECONDS); return work.call(); };
            var first = pool.submit(paired);
            var second = pool.submit(paired);
            return new int[]{first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)};
        }
    }
    @Test void parallelRegistrationAndConsumption() throws Exception {
        assertThat(parallel(() -> mvc.perform(register(email, "Jonas", password)).andReturn().getResponse().getStatus()))
                .containsExactly(202, 202);
        verify(mail, times(1)).send(eq(email), anyString());
        assertThat(parallel(() -> mvc.perform(proof(token.get())).andReturn().getResponse().getStatus()))
                .containsExactlyInAnyOrder(204, 400);
        mvc.perform(proof(token.get())).andExpect(status().isBadRequest());
    }
    @Test void replacesAndExpiresLinks() throws Exception {
        enroll();
        String first = token.get();
        var address = Map.of("email", email);
        mvc.perform(postJson("/auth/resend", address)).andExpect(status().isTooManyRequests());
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        mvc.perform(postJson("/auth/resend", address)).andExpect(status().isAccepted());
        mvc.perform(proof(first)).andExpect(status().isBadRequest());
        when(clock.instant()).thenReturn(start.plusSeconds(60 + 86400));
        mvc.perform(proof(token.get())).andExpect(status().isBadRequest());
    }
    @Test void failedDeliveryCanBeRetried() throws Exception {
        when(mail.send(anyString(), anyString())).thenReturn(false);
        mvc.perform(register(email, "Jonas", password)).andExpect(status().isServiceUnavailable());
        UUID id = accounts.findByEmail(email).orElseThrow().id;
        when(clock.instant()).thenReturn(start.plusSeconds(61));
        when(mail.send(anyString(), anyString())).thenAnswer(call -> { token.set(call.getArgument(1)); return true; });
        mvc.perform(postJson("/auth/resend", Map.of("email", email))).andExpect(status().isAccepted());
        mvc.perform(proof(token.get())).andExpect(status().isNoContent());
        assertThat(accounts.findByEmail(email).orElseThrow().id).isEqualTo(id);
    }
    @Test void ownerIsolationIncludingLegacyRows() throws Exception {
        Task legacy = new Task();
        legacy.name = "Alt";
        tasks.save(legacy);
        var me = signedIn();
        mvc.perform(get("/todos").session(me)).andExpect(content().json("[]"));
        var created = mvc.perform(postJson("/todos", Map.of("name", "Seminar")).session(me))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String path = "/todos/" + json.readTree(created.getContentAsString()).get("id").asLong();
        email = "second-" + email;
        var other = signedIn();
        mvc.perform(get("/todos").session(other)).andExpect(content().json("[]"));
        mvc.perform(get(path).session(other)).andExpect(status().isNotFound());
        mvc.perform(put(path).session(other).with(csrf()).contentType("application/json").content("{\"name\":\"Fremd\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(path).session(other).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get(path).session(me)).andExpect(jsonPath("$.name").value("Seminar"));
    }
    @Test void csrfRequiredAndLogoutInvalidates() throws Exception {
        var session = signedIn();
        mvc.perform(post("/todos").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/auth/logout").session(session)).andExpect(status().isForbidden());
        String csrfBody = mvc.perform(get("/auth/csrf").session(session)).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/auth/logout").session(session).header("X-CSRF-TOKEN", json.readTree(csrfBody).get("token").asString()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/todos")).andExpect(status().isUnauthorized());
    }
    @Test void idleSessionExpires() throws Exception {
        var session = signedIn();
        when(clock.instant()).thenReturn(start.plusSeconds(7200));
        mvc.perform(get("/todos").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void activeSessionExpiresAfterOneDay() throws Exception {
        var session = signedIn();
        for (int hour = 1; hour < 24; hour++) {
            when(clock.instant()).thenReturn(start.plusSeconds(hour * 3600L));
            mvc.perform(get("/todos").session(session)).andExpect(status().isOk());
        }
        when(clock.instant()).thenReturn(start.plusSeconds(86400));
        mvc.perform(get("/todos").session(session)).andExpect(status().isUnauthorized());
    }
}
