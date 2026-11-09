package com.example.demo.common;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.demo.task.TaskNotFoundException;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Wandelt alle Fehler in eine einheitliche JSON-Antwort nach RFC 9457 ("Problem Details") um:
 * <pre>
 * { "type": "about:blank", "title": "Bad Request", "status": 400,
 *   "detail": "Die Eingaben sind ungültig.", "instance": "/todos",
 *   "errors": [ { "field": "title", "message": "Bitte gib einen Titel ein." } ] }
 * </pre>
 * Die Basisklasse ResponseEntityExceptionHandler liefert dieses Format bereits für
 * Spring-eigene Fehler (z. B. 405, 415); hier kommen deutsche Texte und das Feld "errors" dazu.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Eine Feldfehlermeldung, z. B. title -> "Bitte gib einen Titel ein." */
    public record FieldError(String field, String message) {
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleNotFound(TaskNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Verletzte @NotBlank/@Size-Regeln aus TaskRequest oder StatusRequest. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return badRequest("Die Eingaben sind ungültig.", errors, ex, headers, request);
    }

    /** Kaputtes JSON oder ein Wert, der nicht zum Feldtyp passt (z. B. priority "URGENT"). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof DatabindException databind && !databind.getPath().isEmpty()) {
            String field = databind.getPath().getLast().getPropertyName();
            String message = "Der Wert ist ungültig.";
            if (databind instanceof InvalidFormatException invalid && invalid.getTargetType() != null) {
                Class<?> type = invalid.getTargetType();
                if (type.isEnum()) {
                    message = "Erlaubt sind: " + String.join(", ",
                            Arrays.stream(type.getEnumConstants()).map(Object::toString).toList()) + ".";
                } else if (type == java.time.LocalDate.class) {
                    message = "Bitte ein Datum im Format JJJJ-MM-TT angeben.";
                }
            }
            return badRequest("Die Eingaben sind ungültig.", List.of(new FieldError(field, message)), ex,
                    headers, request);
        }
        return badRequest("Der Inhalt der Anfrage ist kein gültiges JSON.", List.of(), ex, headers, request);
    }

    /** Pfadvariable passt nicht zum Typ, z. B. GET /todos/abc. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return badRequest("Die ID muss eine ganze Zahl sein.", List.of(), ex, headers, request);
    }

    /** Alles Unerwartete: 500 ohne interne Details (Stacktrace nur im Server-Log). */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unerwarteter Fehler", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Interner Serverfehler. Bitte später erneut versuchen.");
    }

    private ResponseEntity<Object> badRequest(String detail, List<FieldError> errors, Exception ex,
            HttpHeaders headers, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        if (!errors.isEmpty()) {
            body.setProperties(Map.of("errors", errors));
        }
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }
}
