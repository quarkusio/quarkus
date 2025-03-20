package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import java.util.regex.Pattern;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;

public final class VertxUtil {
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=\"?([^;,\"]+)\"?");
    private static final String FORWARDED = "Forwarded";

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
        return copyUntil(httpServerRequest.getHeader(X_FORWARDED_FOR), ',');
    }

    private static String getHostHeader(HttpServerRequest httpRequest) {
        return copyUntil(vertxGetHeader(httpRequest, HttpHeaderNames.HOST, "host"), ':');
    }

    private static String vertxGetHeader(HttpServerRequest httpRequest, AsciiString vertHeaderName, String headerName) {
        var headers = httpRequest.headers();
        if (headers instanceof HeadersMultiMap) {
            return headers.get(vertHeaderName);
        }
        return headers.get(headerName);
    }

    private static String copyUntil(String s, char c) {
        if (s == null) {
            return null;
        }
        final int first = s.indexOf(c);
        if (first == -1) {
            return "";
        }
        return s.substring(0, first);
    }

    private static Long parseHostPortHeaderAsLong(HttpServerRequest httpRequest) {
        String host = vertxGetHeader(httpRequest, HttpHeaderNames.HOST, "host");
        if (host == null) {
            return null;
        }
        final int startPort = host.indexOf(':');
        // we assume the format host:port
        if (startPort == -1) {
            return null;
        }
        try {
            return Long.parseLong(host, startPort + 1, host.length(), 10);
        } catch (NumberFormatException ignore) {
            // this includes further presence of `:`
            return null;
        }
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
        String hostname = getHostHeader(httpRequest);
        if (hostname != null) {
            return hostname;
        }
        return httpRequest.remoteAddress() != null ? httpRequest.remoteAddress().hostName() : null;
    }

    public static Long extractRemoteHostPort(HttpServerRequest httpRequest) {
        Long portAsString = parseHostPortHeaderAsLong(httpRequest);
        if (portAsString != null) {
            return portAsString;
        }
        if (httpRequest.remoteAddress() != null) {
            return Integer.toUnsignedLong(httpRequest.remoteAddress().port());
        }
        return null;
    }
}
