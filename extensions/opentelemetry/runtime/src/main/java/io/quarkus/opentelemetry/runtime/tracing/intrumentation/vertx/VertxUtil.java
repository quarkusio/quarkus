package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import java.util.regex.Pattern;

import io.vertx.core.http.HttpServerRequest;

public final class VertxUtil {
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=\"?([^;,\"]+)\"?");
    private static final String FORWARDED = "Forwarded";
    private static final String COMMA_SPLITTER = ",";
    private static final char COLON_SPLITTER = ':';
    private static final int SPLIT_LIMIT = -1;

    private VertxUtil() {
    }

    private static String getForwardedHeaderValue(HttpServerRequest httpServerRequest) {
        var forwardedHeader = httpServerRequest.getHeader(FORWARDED);
        if (forwardedHeader == null) {
            return null;
        }
        var forwardedHeaderMatcher = FORWARDED_FOR_PATTERN.matcher(forwardedHeader);
        if (forwardedHeaderMatcher.find()) {
            return forwardedHeaderMatcher.group(1).trim();
        }
        return null;
    }

    private static String getXForwardedHeaderValue(HttpServerRequest httpServerRequest) {
        var xForwardedForHeader = httpServerRequest.getHeader(X_FORWARDED_FOR);
        if (xForwardedForHeader == null) {
            return null;
        }
        return xForwardedForHeader.split(COMMA_SPLITTER, SPLIT_LIMIT)[0];
    }

    public static String extractClientIP(HttpServerRequest httpServerRequest) {
        // Tries to fetch Forwarded first since X-Forwarded can be lost by a proxy
        // If Forwarded is not there tries to fetch the X-Forwarded-For header
        // If none is found resorts to the remote address from the http request

        var forwardedHeaderValue = getForwardedHeaderValue(httpServerRequest);
        if (forwardedHeaderValue != null) {
            return forwardedHeaderValue;
        }
        var xForwardedHeaderValue = getXForwardedHeaderValue(httpServerRequest);
        if (xForwardedHeaderValue != null) {
            return xForwardedHeaderValue;
        }
        return httpServerRequest.remoteAddress() != null ? httpServerRequest.remoteAddress().host() : null;
    }

    public static String extractRemoteHostname(HttpServerRequest httpRequest) {
        String header = httpRequest.getHeader("host");
        if (header == null) {
            return null;
        }

        // localhost:8808, hostname localhost
        String hostname = beforeDelimiter(header, COLON_SPLITTER);

        if (hostname != null) {
            return hostname;
        }
        return httpRequest.remoteAddress() != null ? httpRequest.remoteAddress().hostName() : null;
    }

    public static Long extractRemoteHostPort(HttpServerRequest httpRequest) {
        String header = httpRequest.getHeader("host");
        if (header == null) {
            return null;
        }

        // localhost:8808, port 8080
        String portString = afterDelimiter(header, COLON_SPLITTER);

        if (portString != null) {
            try {
                return Long.parseLong(portString);
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        if (httpRequest.remoteAddress() != null) {
            return Integer.toUnsignedLong(httpRequest.remoteAddress().port());
        }
        return null;
    }

    private static String beforeDelimiter(String str, char delimiter) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        int index = str.indexOf(delimiter);
        return (index >= 0) ? str.substring(0, index) : str;
    }

    private static String afterDelimiter(String str, char delimiter) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        int index = str.indexOf(delimiter);
        if (index < 0) {
            return null;
        } else if (index >= 0 && index + 1 < str.length()) {
            return str.substring(index + 1);
        } else {
            return "";
        }
    }
}
