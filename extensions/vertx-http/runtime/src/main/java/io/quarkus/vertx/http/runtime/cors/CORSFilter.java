package io.quarkus.vertx.http.runtime.cors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    final List<Pattern> allowedOriginsRegex;
    private final List<HttpMethod> configuredHttpMethods;

    public CORSFilter(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
        this.allowedOriginsRegex = parseAllowedOriginsRegex(this.corsConfig.origins);
        configuredHttpMethods = createConfiguredHttpMethods(this.corsConfig.methods);
    }

    private List<HttpMethod> createConfiguredHttpMethods(Optional<List<String>> methods) {
        if (methods.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> corsConfigMethods = methods.get();
        List<HttpMethod> result = new ArrayList<>(corsConfigMethods.size());
        for (String value : corsConfigMethods) {
            result.add(HttpMethod.valueOf(value));
        }
        return result;
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
            Map<String, String> requestedHeaders;
            String[] allowedParts = COMMA_SEPARATED_SPLIT_REGEX.split(allowHeadersValue);
            requestedHeaders = new HashMap<>();
            for (String requestedHeader : allowedParts) {
                requestedHeaders.put(requestedHeader.toLowerCase(), requestedHeader);
            }

            List<String> corsConfigHeaders = corsConfig.headers.get();
            StringBuilder allowedHeaders = new StringBuilder();
            boolean isFirst = true;
            for (String configHeader : corsConfigHeaders) {
                String configHeaderLowerCase = configHeader.toLowerCase();
                if (requestedHeaders.containsKey(configHeaderLowerCase)) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        allowedHeaders.append(',');
                    }
                    allowedHeaders.append(requestedHeaders.get(configHeaderLowerCase));
                }
            }

            if (allowedHeaders.length() != 0) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders.toString());
            }
        }
    }

    private void processMethods(HttpServerResponse response, String allowMethodsValue) {
        if (isConfiguredWithWildcard(corsConfig.methods)) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {
            String[] allowedMethodsParts = COMMA_SEPARATED_SPLIT_REGEX.split(allowMethodsValue);
            List<String> requestedMethods = new ArrayList<>(allowedMethodsParts.length);
            for (String requestedMethod : allowedMethodsParts) {
                requestedMethods.add(requestedMethod.toLowerCase());
            }

            StringBuilder allowMethods = new StringBuilder();
            boolean isFirst = true;
            for (HttpMethod configMethod : configuredHttpMethods) {
                if (requestedMethods.contains(configMethod.name().toLowerCase())) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        allowMethods.append(',');
                    }
                    allowMethods.append(configMethod.name());
                }
            }

            if (allowMethods.length() != 0) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethods.toString());
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

            if (!allowsOrigin) {
                response.setStatusCode(403);
                response.setStatusMessage("CORS Rejected - Invalid origin");
                response.end();
            } else if (request.method().equals(HttpMethod.OPTIONS) && (requestedHeaders != null || requestedMethods != null)) {
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
