package com.example.demo.task;

import org.springframework.data.jpa.repository.JpaRepository;

/** Datenzugriff auf die Tabelle "task". Die SQL-Abfragen erzeugt Spring Data. */
public interface TaskRepository extends JpaRepository<Task, Long> {
}
