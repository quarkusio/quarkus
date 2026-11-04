package io.quarkus.http.ratelimit;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Sends one request from a Vert.x thread and waits for the response.
 */
final class TestHttp {

    private TestHttp() {
    }

    static HttpResponse<Buffer> send(Vertx vertx, WebClient client, URL url, HttpMethod method, String path,
            Map<String, String> headers, Buffer body) {
        return send(vertx, client, url, method, path, headers, body, Duration.ofSeconds(10));
    }

    static HttpResponse<Buffer> send(Vertx vertx, WebClient client, URL url, HttpMethod method, String path,
            Map<String, String> headers, Buffer body, Duration timeout) {
        CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();
        vertx.runOnContext(ignored -> {
            HttpRequest<Buffer> request = client.request(method, url.getPort(), url.getHost(), path);
            headers.forEach(request::putHeader);
            (body == null ? request.send() : request.sendBuffer(body))
                    .onSuccess(result::complete)
                    .onFailure(result::completeExceptionally);
        });
        try {
            return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static HttpResponse<Buffer> get(Vertx vertx, WebClient client, URL url, String path, Map<String, String> headers) {
        return send(vertx, client, url, HttpMethod.GET, path, headers, null);
    }

    /**
     * Sends one GET from a Vert.x thread without waiting, so that a test can have several requests in flight at once.
     */
    static CompletableFuture<HttpResponse<Buffer>> getAsync(Vertx vertx, WebClient client, URL url, String path) {
        CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();
        vertx.runOnContext(ignored -> client.get(url.getPort(), url.getHost(), path).send()
                .onSuccess(result::complete)
                .onFailure(result::completeExceptionally));
        return result;
    }

    /**
     * @return the headers of a request authenticated with HTTP basic authentication
     */
    static Map<String, String> basic(String user, String password) {
        return Map.of("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Adds the embedded users {@code alice} and {@code bob} (password = user name, role {@code user}) and enables
     * basic authentication.
     */
    static QuarkusExtensionTest withUsers(QuarkusExtensionTest test) {
        return test.overrideConfigKey("quarkus.security.users.embedded.enabled", "true")
                .overrideConfigKey("quarkus.security.users.embedded.plain-text", "true")
                .overrideConfigKey("quarkus.security.users.embedded.users.alice", "alice")
                .overrideConfigKey("quarkus.security.users.embedded.users.bob", "bob")
                .overrideConfigKey("quarkus.security.users.embedded.roles.alice", "user")
                .overrideConfigKey("quarkus.security.users.embedded.roles.bob", "user")
                .overrideConfigKey("quarkus.http.auth.basic", "true");
    }

    /**
     * Adds a pre-authentication rule keyed by the client address.
     */
    static QuarkusExtensionTest ipRule(QuarkusExtensionTest test, String rule, String paths, int permits,
            String window) {
        String prefix = "quarkus.http.rate-limit.rules." + rule + ".";
        return test.overrideConfigKey(prefix + "paths", paths)
                .overrideConfigKey(prefix + "phase", "pre-authentication")
                .overrideConfigKey(prefix + "request-key", "ip")
                .overrideRuntimeConfigKey(prefix + "permits", Integer.toString(permits))
                .overrideRuntimeConfigKey(prefix + "window", window);
    }

    @Path("/")
    public static class Resource {

        @GET
        @Path("{path: .*}")
        public String get() {
            return "ok";
        }

        @POST
        @Path("{path: .*}")
        public String post(String body) {
            return "ok";
        }
    }
}
