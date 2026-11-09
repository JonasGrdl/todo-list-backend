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
}, methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
// Erlaubt sind nur diese drei Methoden und genau diese zwei Origins (keine Wildcards).
// Fremde Origins bekommen beim Preflight automatisch 403.
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
        validateTask(task);
        return service.save(task);
    }

    /** PUT /todos/{id}: Ändert name, description und done eines Tasks. 400 bei leerem Namen, 404 bei unbekannter id. */
    @PutMapping("/todos/{id}")
    public Task updateTodo(@PathVariable Long id, @RequestBody Task task) {
        // Erst validieren (400, kein Datenbankzugriff), dann aktualisieren
        validateTask(task);
        // Die id kommt aus dem Pfad; eine id im Body wird von Jackson ohnehin ignoriert (READ_ONLY)
        return service.update(id, task)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task nicht gefunden"));
    }

    /** Maximale Länge für name und description: entspricht der Spalte varchar(255) in der Datenbank. */
    static final int MAX_LENGTH = 255;

    /**
     * Prüft die Eingaben eines Tasks und wirft bei ungültigen Daten einen Fehler
     * mit HTTP-Status 400 (Bad Request):
     * - name darf nicht fehlen (null) und nicht nur aus Leerzeichen bestehen
     * - name und description dürfen höchstens MAX_LENGTH Zeichen lang sein
     * Ohne diese Prüfung würde die Datenbank zu lange Texte ablehnen und der
     * Client bekäme einen 500er (Serverfehler) statt einer verständlichen 400.
     */
    private static void validateTask(Task task) {
        // isBlank() ist true bei einem leeren Text und bei Text, der nur aus Leerzeichen besteht
        if (task.getName() == null || task.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name darf nicht leer sein");
        }
        if (task.getName().length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name darf höchstens " + MAX_LENGTH + " Zeichen lang sein");
        }
        // description ist optional (darf null sein), aber nicht zu lang
        if (task.getDescription() != null && task.getDescription().length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description darf höchstens " + MAX_LENGTH + " Zeichen lang sein");
        }
    }
}