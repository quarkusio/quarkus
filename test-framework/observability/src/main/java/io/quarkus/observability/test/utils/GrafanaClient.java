package io.quarkus.observability.test.utils;

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

import org.jboss.logging.Logger;

import tools.jackson.databind.ObjectMapper;

public class GrafanaClient {
    private static final Logger LOG = Logger.getLogger(GrafanaClient.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String url;
    private final String prometheusUrl;
    private final String tempoUrl;
    private final String username;
    private final String password;

    public GrafanaClient(String url, String username, String password) {
        this(url, null, null, username, password);
    }

    public GrafanaClient(String url, String prometheusUrl, String tempoUrl, String username, String password) {
        this.url = url;
        this.prometheusUrl = prometheusUrl;
        this.tempoUrl = tempoUrl;
        this.username = username;
        this.password = password;
    }

    private <T> void handle(
            String baseUrl,
            String path,
            Function<HttpRequest.Builder, HttpRequest.Builder> method,
            HttpResponse.BodyHandler<T> bodyHandler,
            BiConsumer<HttpResponse<T>, T> consumer) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path));
            if (username != null && password != null) {
                String credentials = username + ":" + password;
                String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
                builder.header("Authorization", "Basic " + encodedCredentials);
            }
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

    public static String endpoint() {
        GrafanaClient client = new GrafanaClient("http://localhost:8080", null, null);
        return client.grafana();
    }

    private String grafana() {
        AtomicReference<String> ref = new AtomicReference<>();
        handle(
                url,
                "/config/grafana",
                HttpRequest.Builder::GET,
                HttpResponse.BodyHandlers.ofString(),
                (r, b) -> {
                    ref.set(b);
                });
        return ref.get();
    }

    public User user() {
        AtomicReference<User> ref = new AtomicReference<>();
        handle(
                url,
                "/api/user",
                HttpRequest.Builder::GET,
                HttpResponse.BodyHandlers.ofString(),
                (r, b) -> {
                    User user = MAPPER.readValue(b, User.class);
                    ref.set(user);
                });
        User user = ref.get();
        LOG.info("User: " + user);
        return user;
    }

    public QueryResult query(String query) {
        AtomicReference<QueryResult> ref = new AtomicReference<>();
        handle(
                prometheusUrl != null ? prometheusUrl : url,
                "/api/v1/query?query=" + query,
                HttpRequest.Builder::GET,
                HttpResponse.BodyHandlers.ofString(),
                (r, b) -> {
                    QueryResult result = MAPPER.readValue(b, QueryResult.class);
                    ref.set(result);
                });
        QueryResult queryResult = ref.get();
        LOG.info("Query: " + queryResult);
        return queryResult;
    }

    public TempoResult traces(String service, int limit, int spss) {
        AtomicReference<TempoResult> ref = new AtomicReference<>();
        String path = "/api/search?q=%7Bresource.service.name%3D%22"
                + service + "%22%7D&limit=" + limit + "&spss=" + spss;
        handle(
                tempoUrl != null ? tempoUrl : url,
                path,
                HttpRequest.Builder::GET,
                HttpResponse.BodyHandlers.ofString(),
                (r, b) -> {
                    TempoResult result = MAPPER.readValue(b, TempoResult.class);
                    ref.set(result);
                });
        TempoResult tempoResult = ref.get();
        LOG.info("Traces: " + tempoResult);
        return tempoResult;
    }

    public String dashboard(String uid) {
        AtomicReference<String> ref = new AtomicReference<>();
        handle(
                url,
                "/api/dashboards/uid/" + uid,
                HttpRequest.Builder::GET,
                HttpResponse.BodyHandlers.ofString(),
                (r, b) -> {
                    ref.set(b);
                });
        String result = ref.get();
        LOG.info("Dashboard: " + result);
        return result;
    }

}
