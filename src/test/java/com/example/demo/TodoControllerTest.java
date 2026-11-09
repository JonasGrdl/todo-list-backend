package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TodoControllerTest {

    private TaskService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(TaskService.class);
        mvc = MockMvcBuilders.standaloneSetup(new TodoController(service)).build();
    }

    // Hilfsmethode: Task mit gesetzter id (es gibt bewusst kein setId())
    static Task task(Long id, String name, String description, boolean done) {
        Task t = new Task(name);
        t.setDescription(description);
        t.setDone(done);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    @Test
    void getTodos_includesId() throws Exception {
        when(service.getAll()).thenReturn(List.of(task(1L, "Einkaufen", "Milch", false)));

        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Einkaufen"));
    }

    @Test
    void getTodoById_found() throws Exception {
        when(service.getById(1L)).thenReturn(Optional.of(task(1L, "Einkaufen", "Milch", false)));

        mvc.perform(get("/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Einkaufen"))
                .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    void getTodoById_notFound() throws Exception {
        when(service.getById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/todos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTodo_ignoresIdInBody() throws Exception {
        when(service.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"name\":\"Neu\",\"description\":\"\",\"done\":false}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getName()).isEqualTo("Neu");
    }

    @Test
    void createTodo_validName_returns201() throws Exception {
        when(service.save(any(Task.class))).thenReturn(task(1L, "Lernen", "Kapitel 3", false));

        mvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lernen\",\"description\":\"Kapitel 3\",\"done\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Lernen"));
    }

    @Test
    void createTodo_emptyName_returns400() throws Exception {
        mvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"x\",\"done\":false}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).save(any());
    }

    @Test
    void createTodo_blankName_returns400() throws Exception {
        mvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"description\":\"x\",\"done\":false}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).save(any());
    }

    @Test
    void createTodo_missingName_returns400() throws Exception {
        mvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"done\":false}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).save(any());
    }
}
