package com.example.demo;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByOwner_Id(UUID owner);
    Optional<Task> findByIdAndOwner_Id(long id, UUID owner);
}
