package app.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class EmulatorApi {
    // Подставь значения из api_info.pdf
    public static String BASE_URL = "http://127.0.0.1:8080";
    public static String GET_DATA_PATH = "/api/getData";
    public static String SUBMIT_RESULT_PATH = "/api/submitResult";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ClientPayload fetchClientData() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + GET_DATA_PATH))
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("GET failed: " + resp.statusCode() + " " + resp.body());
        }
        return MAPPER.readValue(resp.body(), ClientPayload.class);
    }

    public static void sendTestResult(TestResult result) throws Exception {
        String json = MAPPER.writeValueAsString(result);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + SUBMIT_RESULT_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("POST failed: " + resp.statusCode() + " " + resp.body());
        }
    }

    // DTO — подкорректируй поля под фактическую схему JSON из api_info.pdf
    public static class ClientPayload {
        public String email;
        // другие поля клиента при необходимости
    }

    public static class TestResult {
        public String email;
        public boolean formatOk;
        public boolean domainOk;
        public String message;

        public TestResult() {}
        public TestResult(String email, boolean formatOk, boolean domainOk, String message) {
            this.email = email;
            this.formatOk = formatOk;
            this.domainOk = domainOk;
            this.message = message;
        }
    }
}
