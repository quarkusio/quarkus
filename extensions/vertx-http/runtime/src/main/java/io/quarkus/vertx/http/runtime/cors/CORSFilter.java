package io.quarkus.vertx.http.runtime.cors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    final List<Pattern> allowedOriginsRegex;

    public CORSFilter(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
        this.allowedOriginsRegex = parseAllowedOriginsRegex(this.corsConfig.origins);
    }

    public static boolean isConfiguredWithWildcard(Optional<List<String>> optionalList) {
        if (optionalList == null || !optionalList.isPresent()) {
            return true;
        }

        List<String> list = optionalList.get();
        return list.isEmpty() || (list.size() == 1 && "*".equals(list.get(0)));
    }

    /**
     * Parse the provided allowed origins for any regexes
     * 
     * @param allowedOrigins
     * @return a list of compiled regular expressions. If none configured, and empty list is returned
     */
    public static List<Pattern> parseAllowedOriginsRegex(Optional<List<String>> allowedOrigins) {
        if (allowedOrigins == null || !allowedOrigins.isPresent()) {
            return Collections.emptyList();
        }

        // extract configured origins and find any Regular Expressions
        List<Pattern> allowOriginsRegex = new ArrayList<>();
        for (String o : allowedOrigins.get()) {
            if (o != null && o.startsWith("/") && o.endsWith("/")) {
                allowOriginsRegex.add(Pattern.compile(o.substring(1, o.length() - 1)));
            }
        }

        return allowOriginsRegex;
    }

    /**
     * If any regular expression origins are configured, try to match on them.
     * Regular expressions must begin and end with '/'
     * 
     * @param allowedOrigins the configured regex origins.
     * @param origin the specified origin
     * @return true if any configured regular expressions match the specified origin, false otherwise
     */
    public static boolean isOriginAllowedByRegex(List<Pattern> allowOriginsRegex, String origin) {
        if (allowOriginsRegex == null) {
            return false;
        }
        for (Pattern pattern : allowOriginsRegex) {
            if (pattern.matcher(origin).matches()) {
                return true;
            }
        }
        return false;
    }

    private void processRequestedHeaders(HttpServerResponse response, String allowHeadersValue) {
        if (isConfiguredWithWildcard(corsConfig.headers)) {
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
        if (isConfiguredWithWildcard(corsConfig.methods)) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {
            List<String> requestedMethods = new ArrayList<>();
            for (String requestedMethod : COMMA_SEPARATED_SPLIT_REGEX.split(allowMethodsValue)) {
                requestedMethods.add(requestedMethod.toLowerCase());
            }

            List<String> validRequestedMethods = new ArrayList<>();
            List<HttpMethod> methods = corsConfig.methods.get().stream().map(HttpMethod::valueOf)
                    .collect(Collectors.toList());
            for (HttpMethod configMethod : methods) {
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

            boolean allowsOrigin = isConfiguredWithWildcard(corsConfig.origins) || corsConfig.origins.get().contains(origin)
                    || isOriginAllowedByRegex(allowedOriginsRegex, origin);

            if (allowsOrigin) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }

            boolean allowCredentials = corsConfig.accessControlAllowCredentials
                    .orElseGet(() -> corsConfig.origins.isPresent() && corsConfig.origins.get().contains(origin)
                            && !origin.equals("*"));

            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(allowCredentials));

            final Optional<List<String>> exposedHeaders = corsConfig.exposedHeaders;

            if (!isConfiguredWithWildcard(exposedHeaders)) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        String.join(",", exposedHeaders.orElse(Collections.emptyList())));
            }

            if (request.method().equals(HttpMethod.OPTIONS) && (requestedHeaders != null || requestedMethods != null)) {
                if (corsConfig.accessControlMaxAge.isPresent()) {
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
