package io.quarkus.spring.cloud.config.client.runtime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SpringCloudConfigServerResource implements QuarkusTestResourceLifecycleManager {
    private HttpServer httpServer;

    @Override
    public Map<String, String> start() {
        int port = 8089;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/base/a-bootiful-client/test", new SpringCloudConfigServerHandler("config.json"));
            httpServer.createContext("/base/a-bootiful-client/prod", new SpringCloudConfigServerHandler("config.json"));
            httpServer.createContext("/base/a-bootiful-client/common",
                    new SpringCloudConfigServerHandler("config-common.json"));
            httpServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Map.of(
                "quarkus.spring-cloud-config.url", "http://localhost:" + port + "/base",
                "quarkus.spring-cloud-config.username", "user",
                "quarkus.spring-cloud-config.password", "pass",
                "quarkus.spring-cloud-config.enabled", "true",
                "quarkus.spring-cloud-config.headers.h1", "v1",
                "quarkus.spring-cloud-config.headers.h2", "v2");
    }

    @Override
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private static class SpringCloudConfigServerHandler implements HttpHandler {
        private final String resource;

        public SpringCloudConfigServerHandler(final String resource) {
            this.resource = resource;
        }

        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(this.resource);
            if (resource == null) {
                exchange.sendResponseHeaders(400, 0);
                return;
            }

            if (!"v1".equals(exchange.getRequestHeaders().getFirst("h1"))) {
                exchange.sendResponseHeaders(400, 0);
            }
            if (!"v2".equals(exchange.getRequestHeaders().getFirst("h2"))) {
                exchange.sendResponseHeaders(400, 0);
            }

            String body = IOUtils.toString(resource, StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length());
            exchange.getResponseBody().write(body.getBytes());
            exchange.getResponseBody().close();
        }
    }
}
