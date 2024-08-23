package io.quarkus.observability.test.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GrafanaClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String url;
    private final String username;
    private final String password;

    public GrafanaClient(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    private <T> void handle(
            String path,
            Function<HttpRequest.Builder, HttpRequest.Builder> method,
            HttpResponse.BodyHandler<T> bodyHandler,
            BiConsumer<HttpResponse<T>, T> consumer) {
        try {
            String credentials = username + ":" + password;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url + path))
                    .header("Authorization", "Basic " + encodedCredentials);
            HttpRequest request = method.apply(builder).build();

            HttpResponse<T> response = httpClient.send(request, bodyHandler);
            int code = response.statusCode();
            if (code < 200 || code > 299) {
                throw new IllegalStateException("Bad response: " + code + " >> " + response.body());
            }
            consumer.accept(response, response.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public User user() {
        AtomicReference<User> ref = new AtomicReference<>();
        handle(
                "/api/user",
                HttpRequest.Builder::GET,
                HttpResponse.BodyHandlers.ofString(),
                (r, b) -> {
                    try {
                        User user = MAPPER.readValue(b, User.class);
                        ref.set(user);
                    } catch (JsonProcessingException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return ref.get();
    }

    public QueryResult query(String query) {
        AtomicReference<QueryResult> ref = new AtomicReference<>();
        handle(
                "/api/datasources/proxy/1/api/v1/query?query=" + query,
                HttpRequest.Builder::GET,
                HttpResponse.BodyHandlers.ofString(),
                (r, b) -> {
                    try {
                        QueryResult result = MAPPER.readValue(b, QueryResult.class);
                        ref.set(result);
                    } catch (JsonProcessingException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return ref.get();
    }
}
