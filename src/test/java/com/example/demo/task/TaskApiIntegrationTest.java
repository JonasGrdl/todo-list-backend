package com.example.demo.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.TestcontainersConfiguration.MutableClock;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Integrationstests über die komplette Kette Controller -> Service -> Repository -> PostgreSQL.
 * Die Datenbank ist eine isolierte Testcontainers-Instanz; Flyway legt das Schema an,
 * Hibernate prüft es (ddl-auto=validate). Persistenz wird zusätzlich direkt über das
 * Repository geprüft, nicht nur über die HTTP-Antwort.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TaskApiIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-11-09T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-09T09:30:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    TaskRepository repository;

    @Autowired
    MutableClock clock;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
        clock.set(T0);
    }

    // ---------- Hilfsmethoden ----------

    private ResultActions postJson(String body) throws Exception {
        return mvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putJson(long id, String body) throws Exception {
        return mvc.perform(put("/todos/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions patchStatus(long id, String body) throws Exception {
        return mvc.perform(patch("/todos/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createTask(String title) throws Exception {
        String response = postJson("{\"title\":\"" + title + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("id").asLong();
    }

    private static void expectProblem(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ---------- UC-01 Erstellen ----------

    @Nested
    class Create {

        @Test
        void returns201WithLocationAndServerManagedFields() throws Exception {
            postJson("""
                    {"id": 999, "title": "  Übungsblatt 4  ", "description": "Aufgabe 2",
                     "priority": "HIGH", "dueDate": "2026-11-20",
                     "createdAt": "2000-01-01T00:00:00Z", "updatedAt": "2000-01-01T00:00:00Z"}
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/todos/")))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.title").value("Übungsblatt 4"))
                    .andExpect(jsonPath("$.description").value("Aufgabe 2"))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.dueDate").value("2026-11-20"))
                    .andExpect(jsonPath("$.createdAt").value("2026-11-09T08:00:00Z"))
                    .andExpect(jsonPath("$.updatedAt").value("2026-11-09T08:00:00Z"));

            Task saved = repository.findAll().getFirst();
            assertThat(saved.getId()).isNotEqualTo(999L);
            assertThat(saved.getTitle()).isEqualTo("Übungsblatt 4");
            assertThat(saved.getCreatedAt()).isEqualTo(T0);
        }

        @Test
        void appliesDefaultsWhenOptionalFieldsAreMissing() throws Exception {
            postJson("{\"title\":\"Lesen\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.description").doesNotExist())
                    .andExpect(jsonPath("$.dueDate").doesNotExist());
        }

        @Test
        void acceptsLegacyFieldName() throws Exception {
            postJson("{\"name\":\"Alter Client\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Alter Client"));
        }

        @Test
        void allowsPastDueDate() throws Exception {
            postJson("{\"title\":\"Vergessen\",\"dueDate\":\"2020-01-01\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.dueDate").value("2020-01-01"));
        }

        @Test
        void generatesDistinctIds() throws Exception {
            long a = createTask("A");
            long b = createTask("B");
            assertThat(a).isNotEqualTo(b);
        }
    }

    // ---------- Validierung und Feldgrenzen ----------

    @Nested
    class Validation {

        @ParameterizedTest
        @ValueSource(strings = {"{\"title\":\"\"}", "{\"title\":\"   \"}", "{\"description\":\"x\"}", "{\"title\":null}"})
        void rejectsMissingOrBlankTitle(String body) throws Exception {
            ResultActions result = postJson(body);
            expectProblem(result, 400);
            result.andExpect(jsonPath("$.errors[0].field").value("title"))
                    .andExpect(jsonPath("$.errors[0].message").value("Bitte gib einen Titel ein."));
            assertThat(repository.count()).isZero();
        }

        @Test
        void titleLimitIs120CharactersAfterTrimming() throws Exception {
            postJson("{\"title\":\"  " + "a".repeat(120) + "  \"}").andExpect(status().isCreated());

            ResultActions tooLong = postJson("{\"title\":\"" + "a".repeat(121) + "\"}");
            expectProblem(tooLong, 400);
            tooLong.andExpect(jsonPath("$.errors[0].field").value("title"))
                    .andExpect(jsonPath("$.errors[0].message").value("Der Titel darf höchstens 120 Zeichen lang sein."));
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void descriptionLimitIs2000Characters() throws Exception {
            postJson("{\"title\":\"ok\",\"description\":\"" + "b".repeat(2000) + "\"}")
                    .andExpect(status().isCreated());

            ResultActions tooLong = postJson("{\"title\":\"ok\",\"description\":\"" + "b".repeat(2001) + "\"}");
            expectProblem(tooLong, 400);
            tooLong.andExpect(jsonPath("$.errors[0].field").value("description"));
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void rejectsUnknownPriority() throws Exception {
            ResultActions result = postJson("{\"title\":\"ok\",\"priority\":\"URGENT\"}");
            expectProblem(result, 400);
            result.andExpect(jsonPath("$.errors[0].field").value("priority"))
                    .andExpect(jsonPath("$.errors[0].message").value("Erlaubt sind: LOW, MEDIUM, HIGH."));
        }

        @Test
        void rejectsUnknownStatus() throws Exception {
            ResultActions result = postJson("{\"title\":\"ok\",\"status\":\"WAITING\"}");
            expectProblem(result, 400);
            result.andExpect(jsonPath("$.errors[0].field").value("status"));
        }

        @Test
        void rejectsInvalidDate() throws Exception {
            ResultActions result = postJson("{\"title\":\"ok\",\"dueDate\":\"31.12.2026\"}");
            expectProblem(result, 400);
            result.andExpect(jsonPath("$.errors[0].field").value("dueDate"));
        }

        @Test
        void rejectsMalformedJson() throws Exception {
            expectProblem(postJson("{\"title\": "), 400);
        }

        @Test
        void updateIsValidatedToo() throws Exception {
            long id = createTask("Original");
            expectProblem(putJson(id, "{\"title\":\" \"}"), 400);
            assertThat(repository.findById(id)).get().extracting(Task::getTitle).isEqualTo("Original");
        }
    }

    // ---------- UC-02 Übersicht ----------

    @Test
    void listReturnsNewestFirst() throws Exception {
        createTask("Erste");
        clock.set(T1);
        createTask("Zweite");

        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Zweite"))
                .andExpect(jsonPath("$[1].title").value("Erste"));
    }

    @Test
    void getByIdReturnsTask() throws Exception {
        long id = createTask("Einzeln");
        mvc.perform(get("/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Einzeln"));
    }

    // ---------- UC-03 Bearbeiten ----------

    @Test
    void updateReplacesFieldsKeepsStatusAndCreatedAt() throws Exception {
        long id = createTask("Alt");
        patchStatus(id, "{\"status\":\"DONE\"}").andExpect(status().isOk());
        clock.set(T1);

        putJson(id, """
                {"title": "Neu", "description": "Details", "priority": "LOW", "dueDate": "2026-12-01",
                 "createdAt": "2000-01-01T00:00:00Z"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Neu"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-09T08:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-11-09T09:30:00Z"));

        Task reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Neu");
        assertThat(reloaded.getDescription()).isEqualTo("Details");
        assertThat(reloaded.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(reloaded.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(reloaded.getCreatedAt()).isEqualTo(T0);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void updateCanClearOptionalFields() throws Exception {
        String response = postJson("{\"title\":\"X\",\"description\":\"weg\",\"dueDate\":\"2026-11-11\",\"priority\":\"HIGH\"}")
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).get("id").asLong();

        putJson(id, "{\"title\":\"X\",\"description\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.dueDate").doesNotExist())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    // ---------- UC-04 Abschließen und wieder öffnen ----------

    @Test
    void statusCanBeToggledAndIsPersisted() throws Exception {
        long id = createTask("Abhaken");
        clock.set(T1);

        patchStatus(id, "{\"status\":\"DONE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.updatedAt").value("2026-11-09T09:30:00Z"));
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);

        patchStatus(id, "{\"status\":\"OPEN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    void statusChangeRequiresValidValue() throws Exception {
        long id = createTask("Status");
        ResultActions missing = patchStatus(id, "{}");
        expectProblem(missing, 400);
        missing.andExpect(jsonPath("$.errors[0].field").value("status"));

        expectProblem(patchStatus(id, "{\"status\":\"done-ish\"}"), 400);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    // ---------- UC-05 Löschen ----------

    @Test
    void deleteRemovesTaskPermanently() throws Exception {
        long id = createTask("Weg damit");
        long other = createTask("Bleibt");

        mvc.perform(delete("/todos/" + id)).andExpect(status().isNoContent());

        assertThat(repository.existsById(id)).isFalse();
        assertThat(repository.existsById(other)).isTrue();
        expectProblem(mvc.perform(get("/todos/" + id)), 404);
        expectProblem(mvc.perform(delete("/todos/" + id)), 404);
    }

    // ---------- Unbekannte und ungültige IDs ----------

    @Test
    void unknownIdsReturn404Everywhere() throws Exception {
        expectProblem(mvc.perform(get("/todos/424242")), 404);
        expectProblem(putJson(424242, "{\"title\":\"x\"}"), 404);
        expectProblem(patchStatus(424242, "{\"status\":\"DONE\"}"), 404);
        ResultActions deleted = mvc.perform(delete("/todos/424242"));
        expectProblem(deleted, 404);
        deleted.andExpect(jsonPath("$.detail").value("Aufgabe mit der ID 424242 wurde nicht gefunden."));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        ResultActions result = mvc.perform(get("/todos/abc"));
        expectProblem(result, 400);
        result.andExpect(jsonPath("$.detail").value("Die ID muss eine ganze Zahl sein."));
    }

    // ---------- CORS und Betrieb ----------

    @Test
    void corsAllowsConfiguredFrontendForAllMethods() throws Exception {
        mvc.perform(options("/todos/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/todos")
                        .header("Origin", "https://unbekannt.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
