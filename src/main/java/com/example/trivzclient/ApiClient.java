package com.example.trivzclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static String getRaw(String path) throws Exception {
        return send("GET", path, null, true);
    }

    public static <T> T get(String path, Class<T> type) throws Exception {
        String body = send("GET", path, null, true);
        return MAPPER.readValue(body, type);
    }

    public static <T> T get(String path, TypeReference<T> type) throws Exception {
        String body = send("GET", path, null, true);
        return MAPPER.readValue(body, type);
    }

    public static <T> T post(String path, Object body, Class<T> type, boolean auth) throws Exception {
        String resp = send("POST", path, body, auth);
        if (type == Void.class || resp == null || resp.isBlank()) return null;
        return MAPPER.readValue(resp, type);
    }

    public static <T> T put(String path, Object body, Class<T> type) throws Exception {
        String resp = send("PUT", path, body, true);
        if (type == Void.class || resp == null || resp.isBlank()) return null;
        return MAPPER.readValue(resp, type);
    }

    public static void delete(String path) throws Exception {
        send("DELETE", path, null, true);
    }

    private static String send(String method, String path, Object body, boolean auth) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(ClientSession.baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        if (auth && ClientSession.token != null) {
            b.header("Authorization", "Bearer " + ClientSession.token);
        }

        HttpRequest.BodyPublisher pub = (body == null)
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body));

        switch (method) {
            case "GET" -> b.GET();
            case "POST" -> b.POST(pub);
            case "PUT" -> b.PUT(pub);
            case "DELETE" -> b.DELETE();
        }

        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }
}
