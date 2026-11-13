package io.quarkus.spring.cloud.config.client.runtime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * A Config Server that only answers requests carrying the bearer token issued by its own token endpoint.
 */
public class OidcSpringCloudConfigServerResource implements QuarkusTestResourceLifecycleManager {

    private static final String ACCESS_TOKEN = "the-access-token";
    private static final String CLIENT_ID = "config-client";
    private static final String CLIENT_SECRET = "secret";
    private static final String SCOPE = "config";

    private HttpServer httpServer;

    @Override
    public Map<String, String> start() {
        int port = 8090;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/token", OidcSpringCloudConfigServerResource::token);
            httpServer.createContext("/base/a-bootiful-client/common,test", OidcSpringCloudConfigServerResource::config);
            httpServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Map.of(
                "quarkus.spring-cloud-config.url", "http://localhost:" + port + "/base",
                "quarkus.spring-cloud-config.enabled", "true",
                "quarkus.spring-cloud-config.fail-fast", "true",
                "quarkus.spring-cloud-config.oidc.token-url", "http://localhost:" + port + "/token",
                "quarkus.spring-cloud-config.oidc.client-id", CLIENT_ID,
                "quarkus.spring-cloud-config.oidc.client-secret", CLIENT_SECRET,
                "quarkus.spring-cloud-config.oidc.scope", SCOPE);
    }

    @Override
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private static void token(HttpExchange exchange) throws IOException {
        String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String credentials = Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        if (!"POST".equals(exchange.getRequestMethod())
                || !("Basic " + credentials).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            respond(exchange, 401, "{\"error\":\"invalid_client\"}");
            return;
        }
        if (!form.contains("grant_type=client_credentials") || !form.contains("scope=" + SCOPE)) {
            respond(exchange, 400, "{\"error\":\"invalid_request\"}");
            return;
        }
        respond(exchange, 200, "{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"Bearer\",\"expires_in\":300}");
    }

    private static void config(HttpExchange exchange) throws IOException {
        if (!("Bearer " + ACCESS_TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            respond(exchange, 401, "{\"error\":\"invalid_token\"}");
            return;
        }
        URL resource = Thread.currentThread().getContextClassLoader().getResource("config-common-test.json");
        respond(exchange, 200, IOUtils.toString(resource, StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
