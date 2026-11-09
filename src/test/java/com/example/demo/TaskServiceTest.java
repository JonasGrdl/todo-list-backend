package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TaskServiceTest {

    private TaskRepository repository;
    private TaskService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TaskRepository.class);
        service = new TaskService(repository);
    }

    @Test
    void getById_found_returnsTask() {
        Task t = TodoControllerTest.task(1L, "Einkaufen", "Milch", false);
        when(repository.findById(1L)).thenReturn(Optional.of(t));

        assertThat(service.getById(1L)).containsSame(t);
    }

    @Test
    void getById_missing_returnsEmpty() {
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertThat(service.getById(2L).isEmpty()).isTrue();
    }

    @Test
    void update_existing_copiesFieldsAndSaves() {
        Task existing = TodoControllerTest.task(1L, "Alt", "alt", false);
        Task changes = TodoControllerTest.task(null, "Neu", "neu", true);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Task> result = service.update(1L, changes);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Neu");
        assertThat(result.get().getDescription()).isEqualTo("neu");
        assertThat(result.get().isDone()).isTrue();
        verify(repository).save(existing);
    }

    @Test
    void update_missing_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<Task> result = service.update(99L, TodoControllerTest.task(null, "X", "", false));

        assertThat(result.isEmpty()).isTrue();
        verify(repository, never()).save(any());
    }
}
