package com.example.demo;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class Mailer {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json;
    private final String sender;
    private final String frontend;

    Mailer(ObjectMapper json, @Value("${app.sender}") String sender, @Value("${app.frontend}") String frontend) {
        this.json = json;
        this.sender = sender;
        this.frontend = frontend;
    }

    boolean send(String email, String token) {
        String key = System.getenv("RESEND_API_KEY");
        if (key == null || key.isBlank() || sender.isBlank()) return false;
        try {
            var data = Map.of("from", sender, "to", email, "subject", "ToDoList – E-Mail bestätigen",
                    "text", "Öffne den Link und bestätige deine E-Mail-Adresse innerhalb von 24 Stunden:\n"
                            + frontend + "/#bestaetigen=" + token);
            var request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(8)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status >= 200 && status < 300;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Provider exceptions may contain request data.
        }
        return false;
    }
}
