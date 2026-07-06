package io.quarkus.vertx.http.runtime.filters.accesslog;

import java.nio.charset.StandardCharsets;

import io.quarkus.vertx.http.runtime.AccessLogConfig;
import io.vertx.core.buffer.Buffer;

public final class AccessLogBodySupport {

    public static final String REQUEST_BODY_TOKEN = "%{REQUEST_BODY}";
    public static final String RESPONSE_BODY_TOKEN = "%{RESPONSE_BODY}";

    private AccessLogBodySupport() {
    }

    public static boolean isRequestBodyLoggingEnabled(AccessLogConfig config) {
        return config.logRequestBody() || patternContains(config.pattern(), REQUEST_BODY_TOKEN);
    }

    public static boolean isResponseBodyLoggingEnabled(AccessLogConfig config) {
        return config.logResponseBody() || patternContains(config.pattern(), RESPONSE_BODY_TOKEN);
    }

    private static boolean patternContains(String pattern, String token) {
        return resolveNamedPattern(pattern).contains(token);
    }

    static String resolveNamedPattern(String pattern) {
        return switch (pattern) {
            case "common" -> "%h %l %u %t \"%r\" %s %b";
            case "combined" -> "%h %l %u %t \"%r\" %s %b \"%{i,Referer}\" \"%{i,User-Agent}\"";
            case "long" -> "%r\n%{ALL_REQUEST_HEADERS}";
            default -> pattern;
        };
    }

    public static String formatBody(Buffer buffer, int maxSize) {
        if (buffer == null || buffer.length() == 0) {
            return null;
        }
        return formatBodyBytes(buffer.getBytes(), maxSize);
    }

    public static String formatBodyBytes(byte[] bytes, int maxSize) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (!isText(bytes)) {
            return "<binary, " + bytes.length + " bytes>";
        }
        int length = Math.min(bytes.length, maxSize);
        String body = new String(bytes, 0, length, StandardCharsets.UTF_8);
        if (bytes.length > maxSize) {
            return body + "...(truncated)";
        }
        return body;
    }

    private static boolean isText(byte[] bytes) {
        for (byte b : bytes) {
            if (b < 0x20 && b != '\n' && b != '\r' && b != '\t') {
                return false;
            }
        }
        return true;
    }
}
