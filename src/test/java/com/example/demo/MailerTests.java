package com.example.demo;

import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MailerTests {
    final ObjectMapper json = new ObjectMapper();
    final HttpClient client = mock(HttpClient.class);

    Mailer mailer(String key) {
        return new Mailer(json, "sender@example.org", "https://app.example.org", key, "secret", client);
    }

    @SuppressWarnings("unchecked")
    void response(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test void sendsConfirmationWithMailjetAuthenticationAndTrackingDisabled() throws Exception {
        response(200, "{\"Messages\":[{\"Status\":\"success\"}]}");
        assertThat(mailer("public").send("recipient@example.net", "proof")).isTrue();
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        var request = captor.getValue();
        assertThat(request.uri().toString()).isEqualTo("https://api.mailjet.com/v3.1/send");
        assertThat(request.headers().firstValue("Authorization")).contains("Basic cHVibGljOnNlY3JldA==");
        var body = new CompletableFuture<String>();
        var bytes = new java.io.ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); bytes.writeBytes(chunk);
            }
            public void onError(Throwable failure) { body.completeExceptionally(failure); }
            public void onComplete() { body.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        var message = json.readTree(body.get()).path("Messages").get(0);
        assertThat(message.path("From").path("Email").asString()).isEqualTo("sender@example.org");
        assertThat(message.path("To").get(0).path("Email").asString()).isEqualTo("recipient@example.net");
        assertThat(message.path("TextPart").asString()).contains("https://app.example.org/#bestaetigen=proof");
        assertThat(message.path("TrackClicks").asString()).isEqualTo("disabled");
    }

    @Test void rejectsProviderErrorsIncludingHttpSuccessWithMessageFailure() throws Exception {
        response(200, "{\"Messages\":[{\"Status\":\"error\"}]}");
        assertThat(mailer("public").send("recipient@example.net", "proof")).isFalse();
        response(401, "{}");
        assertThat(mailer("public").send("recipient@example.net", "proof")).isFalse();
        response(200, "not json");
        assertThat(mailer("public").send("recipient@example.net", "proof")).isFalse();
    }

    @Test void missingCredentialsNeverSend() {
        assertThat(mailer("").send("recipient@example.net", "proof")).isFalse();
        verifyNoInteractions(client);
    }
}
