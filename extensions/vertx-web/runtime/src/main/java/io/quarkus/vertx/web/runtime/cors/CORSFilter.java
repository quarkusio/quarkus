package io.quarkus.vertx.web.runtime.cors;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class CORSFilter implements Handler<RoutingContext> {

    // This is set in the recorder at runtime.
    // Must be static because the filter is created(deployed) at build time and runtime config is still not available
    final CORSConfig corsConfig;

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ORIGIN = "Origin";
    private static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    public CORSFilter(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    private void processHeaders(HttpServerResponse response, String requestedHeaders, String allowedHeaders) {
        String validHeaders = Arrays.stream(requestedHeaders.split(","))
                .filter(allowedHeaders::contains)
                .collect(Collectors.joining(","));
        if (!validHeaders.isEmpty())
            response.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, validHeaders);
    }

    private void processMethods(HttpServerResponse response, String requestedMethods, String allowedMethods) {
        String validMethods = Arrays.stream(requestedMethods.split(","))
                .filter(allowedMethods::contains)
                .collect(Collectors.joining(","));
        if (!validMethods.isEmpty())
            response.headers().set(ACCESS_CONTROL_ALLOW_METHODS, validMethods);
    }

    @Override
    public void handle(RoutingContext event) {
        Objects.requireNonNull(corsConfig, "CORS config is not set");
        HttpServerRequest request = event.request();
        HttpServerResponse response = event.response();
        String origin = request.getHeader(ORIGIN);
        if (origin == null) {
            event.next();
        } else {
            String requestedMethods = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);
            if (requestedMethods != null) {
                processMethods(response, requestedMethods, corsConfig.methods.orElse(requestedMethods));
            }
            String requestedHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
            if (requestedHeaders != null) {
                processHeaders(response, requestedHeaders, corsConfig.headers.orElse(requestedHeaders));
            }
            String allowedOrigins = corsConfig.origins.orElse(null);
            boolean allowsOrigin = allowedOrigins == null || allowedOrigins.contains(origin);
            if (allowsOrigin)
                response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.headers().set(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            corsConfig.exposedHeaders.ifPresent(exposed -> response.headers().set(ACCESS_CONTROL_EXPOSE_HEADERS, exposed));
            if (request.method().equals(HttpMethod.OPTIONS)) {
                response.end();
            } else {
                event.next();
            }
        }
    }
}
