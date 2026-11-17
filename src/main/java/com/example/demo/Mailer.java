package com.example.demo;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class Mailer {
    private final HttpClient client;
    private final ObjectMapper json;
    private final String sender;
    private final String frontend;
    private final String apiKey;
    private final String secretKey;

    @Autowired
    Mailer(ObjectMapper json, @Value("${app.sender}") String sender, @Value("${app.frontend}") String frontend,
           @Value("${MAILJET_API_KEY:}") String apiKey, @Value("${MAILJET_SECRET_KEY:}") String secretKey) {
        this(json, sender, frontend, apiKey, secretKey,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build());
    }

    Mailer(ObjectMapper json, String sender, String frontend, String apiKey, String secretKey, HttpClient client) {
        this.json = json;
        this.sender = sender;
        this.frontend = frontend;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.client = client;
    }

    boolean send(String email, String token) {
        if (apiKey.isBlank() || secretKey.isBlank() || sender.isBlank()) return false;
        try {
            var data = Map.of("Messages", List.of(Map.of(
                    "From", Map.of("Email", sender, "Name", "ToDoList"),
                    "To", List.of(Map.of("Email", email)),
                    "Subject", "ToDoList – E-Mail bestätigen",
                    "TrackClicks", "disabled", "TrackOpens", "disabled",
                    "TextPart", "Öffne den Link und bestätige deine E-Mail-Adresse innerhalb von 24 Stunden:\n"
                            + frontend + "/#bestaetigen=" + token)));
            String credentials = Base64.getEncoder().encodeToString(
                    (apiKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(URI.create("https://api.mailjet.com/v3.1/send"))
                    .timeout(Duration.ofSeconds(8)).header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return false;
            var messages = json.readTree(response.body()).path("Messages");
            // Mailjet can report a per-message error even for an HTTP-success response.
            return messages.isArray() && messages.size() == 1
                    && "success".equals(messages.get(0).path("Status").asString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Provider exceptions may contain request data.
        }
        return false;
    }
}
