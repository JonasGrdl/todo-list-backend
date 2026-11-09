package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST-Controller für die Todo-Liste. Nimmt die HTTP-Anfragen entgegen
 * und gibt die Antworten (JSON) zurück. Die eigentliche Arbeit macht TaskService.
 */
@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://todo-list-frontend-165i.onrender.com"
})
public class TodoController {

    private final TaskService service;

    public TodoController(TaskService service) {
        this.service = service;
    }

    /** GET /todos: Gibt alle Tasks als Liste zurück. */
    @GetMapping("/todos")
    public Iterable<Task> getTodos() {
        return service.getAll();
    }

    /** GET /todos/{id}: Gibt genau einen Task zurück, oder 404, wenn die id unbekannt ist. */
    @GetMapping("/todos/{id}")
    public Task getTodo(@PathVariable Long id) {
        // Wenn kein Task gefunden wird, wirft orElseThrow einen Fehler mit Status 404
        return service.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task nicht gefunden"));
    }

    /** POST /todos: Legt einen neuen Task an und gibt ihn mit Status 201 zurück. */
    @PostMapping("/todos")
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTodo(@RequestBody Task task) {
        // Erst prüfen, dann speichern: ein ungültiger Name landet nie in der Datenbank
        requireName(task);
        return service.save(task);
    }

    /**
     * Prüft, ob der Task einen Namen hat. Fehlt der Name (null) oder besteht er nur
     * aus Leerzeichen, wird ein Fehler mit HTTP-Status 400 (Bad Request) geworfen.
     * Die Methode gibt nichts zurück; sie ist nur dazu da, bei ungültigen Daten abzubrechen.
     */
    private static void requireName(Task task) {
        // isBlank() ist true bei einem leeren Text und bei Text, der nur aus Leerzeichen besteht
        if (task.getName() == null || task.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name darf nicht leer sein");
        }
    }
}