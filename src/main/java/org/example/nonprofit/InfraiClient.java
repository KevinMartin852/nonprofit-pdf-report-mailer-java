package org.example.nonprofit;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class InfraiClient {
    private final InfraiConfig config;
    private final HttpClient http;
    public final Email email = new Email();

    public InfraiClient(InfraiConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public final class Email {
        public SendResult send(Map<String, String> message, String idempotencyKey)
                throws IOException, InterruptedException {
            return post(message, idempotencyKey);
        }
    }

    private SendResult post(Map<String, String> body, String idempotencyKey)
            throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(config.baseUri().resolve("/v1/email/send"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(Json.object(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope = Json.parseObject(response.body());
            if (response.statusCode() == 429 && attempt < 4) {
                Thread.sleep(retryDelayMillis(response, attempt++));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Object error = envelope.get("error");
                throw new InfraiException(response.statusCode(), error == null ? "Request rejected" : error.toString());
            }
            Object dataValue = envelope.get("data");
            if (!(dataValue instanceof Map<?, ?> data) || !(data.get("message_id") instanceof String id)) {
                throw new IOException("Successful response did not include message_id");
            }
            return new SendResult(id, envelope.get("metadata"));
        }
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .flatMap(value -> {
                    try { return java.util.Optional.of(Long.parseLong(value) * 1000L); }
                    catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
                })
                .orElse(250L * (1L << attempt));
    }

    public record SendResult(String messageId, Object metadata) {}

    public static final class InfraiException extends IOException {
        private final int statusCode;
        InfraiException(int statusCode, String message) { super(message); this.statusCode = statusCode; }
        public int statusCode() { return statusCode; }
    }
}
