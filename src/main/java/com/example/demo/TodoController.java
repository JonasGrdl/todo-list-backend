package com.example.demo;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/todos")
class TodoController {
    private final TaskService service;
    TodoController(TaskService service) { this.service = service; }

    @GetMapping List<TaskService.View> list(@AuthenticationPrincipal UUID owner) { return service.list(owner); }
    @GetMapping("/{id}") TaskService.View get(@AuthenticationPrincipal UUID owner, @PathVariable long id) {
        return service.get(owner, id);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    TaskService.View create(@AuthenticationPrincipal UUID owner, @Valid @RequestBody TaskService.Draft draft) {
        return service.save(owner, null, draft);
    }
    @PutMapping("/{id}")
    TaskService.View update(@AuthenticationPrincipal UUID owner, @PathVariable long id, @Valid @RequestBody TaskService.Draft draft) {
        return service.save(owner, id, draft);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID owner, @PathVariable long id) { service.delete(owner, id); }
}
