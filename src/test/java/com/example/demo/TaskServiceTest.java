package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
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
}
