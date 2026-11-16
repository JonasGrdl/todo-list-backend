package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

class ApiError extends RuntimeException {
    final int status;
    ApiError(int status, String message) { super(message); this.status = status; }
    static ApiError limited() { return new ApiError(429, "Zu viele Versuche. Bitte versuche es später erneut."); }
}
@RestControllerAdvice
class ApiErrors {
    record Message(String message) {}
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<Message> invalid() {
        return ResponseEntity.badRequest().body(new Message("Bitte überprüfe die Eingaben oder den Bestätigungslink."));
    }
    @ExceptionHandler(ApiError.class)
    ResponseEntity<Message> handle(ApiError e) {
        return ResponseEntity.status(e.status).body(new Message(e.getMessage()));
    }
}
