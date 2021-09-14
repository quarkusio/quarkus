package io.quarkus.opentelemetry.runtime.tracing.vertx;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.vertx.core.http.HttpServerRequest;

public final class VertxUtil {
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=\"?([^;,\"]+)\"?");
    private static final String FORWARDED = "Forwarded";
    private static final String COMMA_SPLITTER = ",";
    private static final int SPLIT_LIMIT = -1;

    private VertxUtil() {
    }

    private static Optional<String> getForwardedHeaderValue(HttpServerRequest httpServerRequest) {
        return Optional.ofNullable(httpServerRequest.getHeader(FORWARDED))
                .map(FORWARDED_FOR_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1).trim());
    }

    private static Optional<String> getXForwardedHeaderValue(HttpServerRequest httpServerRequest) {
        return Optional.ofNullable(httpServerRequest.getHeader(X_FORWARDED_FOR))
                .flatMap(o -> Stream.of(o.split(COMMA_SPLITTER, SPLIT_LIMIT))
                        .findFirst());
    }

    public static String extractClientIP(HttpServerRequest httpServerRequest) {
        // Tries to fetch Forwarded first since X-Forwarded can be lost by a proxy
        // If Forwarded is not there tries to fetch the X-Forwarded-For header
        // If none is found resorts to the remote address from the http request
        return getForwardedHeaderValue(httpServerRequest)
                .orElseGet(() -> getXForwardedHeaderValue(httpServerRequest)
                        .orElseGet(() -> httpServerRequest.remoteAddress().host()));

    }
}
