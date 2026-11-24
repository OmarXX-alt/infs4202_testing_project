package uniplace.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal HTTP client for the Engine. Black-box: it only knows the URL and the JSON contract
 * (engine/openapi.yaml).
 */
final class EngineApi {

    static final String BASE = baseUrl();
    static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private EngineApi() {
    }

    private static String baseUrl() {
        String url = System.getProperty("engine.url");
        if (url == null || url.isBlank() || url.startsWith("${")) {
            url = "http://127.0.0.1:8081/api/v1";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** A response: HTTP status, parsed JSON body (or null) and the elapsed time. */
    record Response(int status, JsonNode body, long millis) {
        boolean bool(String field) {
            return body.get(field).asBoolean();
        }

        int integer(String field) {
            return body.get(field).asInt();
        }
    }

    static Response post(String path, String json) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    static Response get(String path) {
        return send(HttpRequest.newBuilder(URI.create(BASE + path)).timeout(Duration.ofSeconds(10)).GET().build());
    }

    private static Response send(HttpRequest request) {
        long start = System.nanoTime();
        try {
            HttpResponse<String> r = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            long millis = (System.nanoTime() - start) / 1_000_000;
            JsonNode body = r.body() == null || r.body().isBlank() ? null : JSON.readTree(r.body());
            return new Response(r.statusCode(), body, millis);
        } catch (java.net.ConnectException e) {
            throw new IllegalStateException("The Engine is not running at " + BASE
                    + ". Start it with: java -jar engine/uniplace-engine.jar", e);
        } catch (Exception e) {
            throw new IllegalStateException("Engine call failed: " + request.uri(), e);
        }
    }
}
