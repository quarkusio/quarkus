package io.quarkus.vertx.web.runtime.cors;

import java.util.Objects;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class CORSFilter implements Handler<RoutingContext> {

    // This is set in the recorder at runtime.
    // Must be static because the filter is created(deployed) at build time and runtime config is still not available
    final CORSConfig corsConfig;

    public CORSFilter(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    private void processRequestedHeaders(HttpServerResponse response, String requestedHeaders) {
        if (corsConfig.headers.isEmpty()) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders);
        } else {
            final String validRequestHeaders = corsConfig.headers
                    .stream()
                    .filter(requestedHeaders::contains)
                    .collect(Collectors.joining(","));

            if (!validRequestHeaders.isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, validRequestHeaders);
            }
        }
    }

    private void processMethods(HttpServerResponse response, String requestedMethods) {
        if (corsConfig.methods.isEmpty()) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestedMethods);
        } else {

            final String validRequestedMethods = corsConfig.methods
                    .stream()
                    .filter(method -> requestedMethods.contains(method.name()))
                    .map(HttpMethod::name)
                    .collect(Collectors.joining(","));

            if (!validRequestedMethods.isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, validRequestedMethods);
            }
        }
    }

    @Override
    public void handle(RoutingContext event) {
        Objects.requireNonNull(corsConfig, "CORS config is not set");
        HttpServerRequest request = event.request();
        HttpServerResponse response = event.response();
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            event.next();
        } else {
            final String requestedMethods = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);

            if (requestedMethods != null) {
                processMethods(response, requestedMethods);
            }

            final String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

            if (requestedHeaders != null) {
                processRequestedHeaders(response, requestedHeaders);
            }

            boolean allowsOrigin = corsConfig.origins.isEmpty() || corsConfig.origins.contains(origin);

            if (allowsOrigin) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }

            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

            final String exposedHeaders = corsConfig.exposedHeaders.stream().collect(Collectors.joining(","));

            if (!exposedHeaders.isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
            }

            if (request.method().equals(HttpMethod.OPTIONS)) {
                if ((requestedHeaders != null || requestedMethods != null) && corsConfig.accessControlMaxAge.isPresent()) {
                    response.putHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                            String.valueOf(corsConfig.accessControlMaxAge.get().getSeconds()));
                }
                response.end();
            } else {
                event.next();
            }
        }
    }
}
