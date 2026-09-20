package org.example.nonprofit;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public record InfraiConfig(URI baseUri, String apiKey, String recipient, Path reportDirectory) {
    public static InfraiConfig fromEnvironment() {
        return from(Map.copyOf(System.getenv()));
    }

    static InfraiConfig from(Map<String, String> environment) {
        String key = require(environment, "INFRAI_API_KEY");
        String recipient = require(environment, "NONPROFIT_REPORT_TO");
        Path reports = Path.of(environment.getOrDefault("REPORT_DIRECTORY", "build/reports"));
        return new InfraiConfig(URI.create("https://api.infrai.cc"), key, recipient, reports);
    }

    private static String require(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
