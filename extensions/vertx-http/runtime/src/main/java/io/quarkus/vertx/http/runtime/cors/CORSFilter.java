package io.quarkus.vertx.http.runtime.cors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class CORSFilter implements Handler<RoutingContext> {

    private static final Pattern COMMA_SEPARATED_SPLIT_REGEX = Pattern.compile("\\s*,\\s*");

    // This is set in the recorder at runtime.
    // Must be static because the filter is created(deployed) at build time and runtime config is still not available
    final CORSConfig corsConfig;

    public CORSFilter(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    private void processRequestedHeaders(HttpServerResponse response, String allowHeadersValue) {
        if (!corsConfig.headers.isPresent()) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeadersValue);
        } else {
            List<String> requestedHeaders = new ArrayList<>();
            for (String requestedHeader : COMMA_SEPARATED_SPLIT_REGEX.split(allowHeadersValue)) {
                requestedHeaders.add(requestedHeader.toLowerCase());
            }

            List<String> validRequestedHeaders = new ArrayList<>();
            for (String configHeader : corsConfig.headers.get()) {
                if (requestedHeaders.contains(configHeader.toLowerCase())) {
                    validRequestedHeaders.add(configHeader);
                }
            }

            if (!validRequestedHeaders.isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", validRequestedHeaders));
            }
        }
    }

    private void processMethods(HttpServerResponse response, String allowMethodsValue) {
        if (!corsConfig.methods.isPresent()) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {
            List<String> requestedMethods = new ArrayList<>();
            for (String requestedMethod : COMMA_SEPARATED_SPLIT_REGEX.split(allowMethodsValue)) {
                requestedMethods.add(requestedMethod.toLowerCase());
            }

            List<String> validRequestedMethods = new ArrayList<>();
            for (HttpMethod configMethod : corsConfig.methods.get()) {
                if (requestedMethods.contains(configMethod.name().toLowerCase())) {
                    validRequestedMethods.add(configMethod.name());
                }
            }

            if (!validRequestedMethods.isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, String.join(",", validRequestedMethods));
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

            boolean allowsOrigin = !corsConfig.origins.isPresent() || corsConfig.origins.get().contains(origin);

            if (allowsOrigin) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }

            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

            final Optional<List<String>> exposedHeaders = corsConfig.exposedHeaders;

            if (exposedHeaders.isPresent()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        String.join(",", exposedHeaders.orElse(Collections.emptyList())));
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
