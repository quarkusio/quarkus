package io.quarkus.vertx.http.runtime.cors;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class CORSFilter implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(CORSFilter.class);

    private final CORSConfig corsConfig;

    private final boolean wildcardOrigin;
    private final boolean wildcardMethod;
    private final List<Pattern> allowedOriginsRegex;
    private final Set<HttpMethod> configuredHttpMethods;

    private final String exposedHeaders;

    private final String allowedHeaders;

    private final String allowedMethods;

    public CORSFilter(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
        this.wildcardOrigin = isOriginConfiguredWithWildcard(this.corsConfig.origins());
        this.wildcardMethod = isConfiguredWithWildcard(corsConfig.methods());
        this.allowedOriginsRegex = this.wildcardOrigin ? List.of() : parseAllowedOriginsRegex(this.corsConfig.origins());
        this.configuredHttpMethods = createConfiguredHttpMethods(this.corsConfig.methods());
        this.exposedHeaders = createHeaderString(this.corsConfig.exposedHeaders());
        this.allowedHeaders = createHeaderString(this.corsConfig.headers());
        this.allowedMethods = createHeaderString(this.corsConfig.methods());
    }

    private String createHeaderString(Optional<List<String>> headers) {
        if (headers.isEmpty()) {
            return null;
        }
        if (headers.get().isEmpty()) {
            return null;
        }
        if (headers.get().size() == 1 && headers.get().get(0).equals("*")) {
            return null;
        }
        return String.join(",", headers.get());
    }

    private Set<HttpMethod> createConfiguredHttpMethods(Optional<List<String>> methods) {
        if (methods.isEmpty()) {
            return Set.of();
        }
        List<String> corsConfigMethods = methods.get();
        LinkedHashSet<HttpMethod> result = new LinkedHashSet<>(corsConfigMethods.size());
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

    private static boolean isOriginConfiguredWithWildcard(Optional<List<String>> origins) {
        if (origins.isEmpty() || origins.get().size() != 1) {
            return false;
        }

        String origin = origins.get().get(0);

        return "*".equals(origin) || "/.*/".equals(origin);
    }

    /**
     * Parse the provided allowed origins for any regexes
     *
     * @param allowedOrigins
     * @return a list of compiled regular expressions. If none configured, and empty list is returned
     */
    public static List<Pattern> parseAllowedOriginsRegex(Optional<List<String>> allowedOrigins) {
        if (allowedOrigins == null || !allowedOrigins.isPresent()) {
            return List.of();
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
     * @param allowOriginsRegex the configured regex origins.
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

    @Override
    public void handle(RoutingContext event) {
        Objects.requireNonNull(corsConfig, "CORS config is not set");
        HttpServerRequest request = event.request();
        HttpServerResponse response = event.response();
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            event.next();
        } else {

            //for both normal and preflight requests we need to check the origin
            boolean allowsOrigin = wildcardOrigin;
            boolean originMatches = !wildcardOrigin && corsConfig.origins().isPresent() &&
                    (corsConfig.origins().get().contains(origin) || isOriginAllowedByRegex(allowedOriginsRegex, origin));
            if (!allowsOrigin) {
                if (corsConfig.origins().isPresent()) {
                    allowsOrigin = originMatches || isSameOrigin(request, origin);
                } else {
                    allowsOrigin = isSameOrigin(request, origin);
                }
            }
            if (!allowsOrigin) {
                LOG.debugf("Invalid origin %s", origin);
                response.setStatusCode(403);
                response.setStatusMessage("CORS Rejected - Invalid origin");
            } else {
                boolean allowCredentials = corsConfig.accessControlAllowCredentials().orElse(originMatches);
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(allowCredentials));
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }

            if (request.method().equals(HttpMethod.OPTIONS)) {
                final String requestedMethods = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
                final String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
                //preflight request, handle it specially
                if (requestedHeaders != null || requestedMethods != null) {
                    handlePreflightRequest(event, requestedHeaders, requestedMethods, origin, allowsOrigin);
                    response.end();
                    return;
                }
            }
            if (allowedHeaders != null) {
                response.headers().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
            }
            if (allowedMethods != null) {
                response.headers().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
            }

            //always set expose headers if present
            if (exposedHeaders != null) {
                response.headers().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
            }

            //we check that the actual request matches the allowed methods and headers
            if (!isMethodAllowed(request.method())) {
                LOG.debugf("Method %s is not allowed", request.method());
                response.setStatusCode(403);
                response.setStatusMessage("CORS Rejected - Invalid method");
                response.end();
                return;
            }
            if (!allowsOrigin) {
                response.end();
                return;
            }

            //all good, it can proceed
            event.next();
        }
    }

    private void handlePreflightRequest(RoutingContext event, String requestedHeaders, String requestedMethods, String origin,
            boolean allowsOrigin) {
        //see https://fetch.spec.whatwg.org/#http-cors-protocol

        if (corsConfig.accessControlMaxAge().isPresent()) {
            event.response().putHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                    String.valueOf(corsConfig.accessControlMaxAge().get().getSeconds()));
        }
        var response = event.response();
        if (requestedMethods != null) {
            processPreFlightMethods(response, requestedMethods);
        }

        if (requestedHeaders != null) {
            processPreFlightRequestedHeaders(response, requestedHeaders);
        }

        //always set expose headers if present
        if (exposedHeaders != null) {
            response.headers().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
        }

    }

    static boolean isSameOrigin(HttpServerRequest request, String origin) {
        //fast path check, when everything is the same
        if (origin.startsWith(request.scheme())) {
            if (!substringMatch(origin, request.scheme().length(), "://", false)) {
                LOG.debugf(
                        "Same origin check has failed, the origin is not a substring of the request URI. Request URI: %s, origin: %s",
                        request.absoluteURI(), origin);
                return false;
            }
            if (substringMatch(origin, request.scheme().length() + 3, request.host(), true)) {
                //they are a simple match
                return true;
            }
            return isSameOriginSlowPath(request, origin);
        } else {
            return false;
        }
    }

    static boolean isSameOriginSlowPath(HttpServerRequest request, String origin) {
        String absUriString = request.absoluteURI();
        //we already know the scheme is correct, as the fast path will reject that
        URI baseUri = URI.create(absUriString);
        URI originUri;
        try {
            originUri = URI.create(origin);
        } catch (IllegalArgumentException e) {
            LOG.debugf("Malformed origin url: %s", origin);
            return false;
        }

        if (!originUri.getPath().isEmpty()) {
            //origin should not contain a path component
            //just reject it in this case
            LOG.debugf("Same origin check has failed as the origin contains a path component. Request URI: %s, origin: %s",
                    request.absoluteURI(), origin);
            return false;
        }
        if (!baseUri.getHost().equals(originUri.getHost())) {
            LOG.debugf("Same origin check has failed, the host values do not match. Request URI: %s, origin: %s",
                    request.absoluteURI(),
                    origin);
            return false;
        }
        if (baseUri.getPort() == originUri.getPort()) {
            return true;
        }
        if (baseUri.getPort() != -1 && originUri.getPort() != -1) {
            //ports are explictly set
            return false;
        }
        if (baseUri.getScheme().equals("http")) {
            if (baseUri.getPort() == 80 || baseUri.getPort() == -1) {
                if (originUri.getPort() == 80 || originUri.getPort() == -1) {
                    //port is either unset or 80
                    return true;
                }
            }
        } else if (baseUri.getScheme().equals("https")) {
            if (baseUri.getPort() == 443 || baseUri.getPort() == -1) {
                if (originUri.getPort() == 443 || originUri.getPort() == -1) {
                    //port is either unset or 443
                    return true;
                }
            }
        }
        LOG.debugf("Same origin check has failed. Request URI: %s, origin: %s", request.absoluteURI(), origin);
        return false;
    }

    static boolean substringMatch(String str, int pos, String substring, boolean requireFull) {
        int length = str.length();
        int subLength = substring.length();
        int strPos = pos;
        int subPos = 0;
        if (pos + subLength > length) {
            //too long, avoid checking in the loop
            return false;
        }
        for (;;) {
            if (subPos == subLength) {
                //if we are at the end return the correct value, depending on if we are also at the end of the origin
                return !requireFull || strPos == length;
            }
            if (str.charAt(strPos) != substring.charAt(subPos)) {
                return false;
            }
            strPos++;
            subPos++;
        }
    }

    private void processPreFlightRequestedHeaders(HttpServerResponse response, String allowHeadersValue) {
        if (isConfiguredWithWildcard(corsConfig.headers())) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeadersValue);
        } else {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
        }
    }

    private void processPreFlightMethods(HttpServerResponse response, String allowMethodsValue) {
        if (wildcardMethod) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {
            StringBuilder allowMethods = new StringBuilder();
            boolean isFirst = true;
            for (HttpMethod configMethod : configuredHttpMethods) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    allowMethods.append(",");
                }
                allowMethods.append(configMethod.name());

            }
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethods.toString());
        }
    }

    private boolean isMethodAllowed(HttpMethod method) {
        if (wildcardMethod) {
            return true;
        } else {
            return configuredHttpMethods.contains(method);
        }
    }
}
