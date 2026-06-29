package io.quarkus.vertx.http.runtime.filters.accesslog;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

/**
 * Logs invalid HTTP requests rejected by the Vert.x HTTP decoder (e.g. 414, 431) to the access log.
 * <p>
 * These requests never reach the Vert.x Web router, so the regular {@link AccessLogHandler} cannot log them.
 */
public class InvalidRequestAccessLogHandler implements Handler<HttpServerRequest> {

    private static final DateTimeFormatter COMMON_LOG_DATE_FORMAT = DateTimeFormatter
            .ofPattern("'['dd/MMM/yyyy:HH:mm:ss Z']'", Locale.US);

    private final AccessLogReceiver accessLogReceiver;
    private final String pattern;

    public InvalidRequestAccessLogHandler(AccessLogReceiver accessLogReceiver, String pattern) {
        this.accessLogReceiver = accessLogReceiver;
        this.pattern = pattern;
    }

    @Override
    public void handle(HttpServerRequest request) {
        int statusCode = resolveStatusCode(request);
        accessLogReceiver.logMessage(formatAccessLogMessage(request, statusCode));
        HttpServerRequest.DEFAULT_INVALID_REQUEST_HANDLER.handle(request);
    }

    static int resolveStatusCode(HttpServerRequest request) {
        DecoderResult decoderResult = request.decoderResult();
        if (decoderResult != null && decoderResult.cause() instanceof TooLongFrameException tooLong) {
            String message = tooLong.getMessage();
            if (message != null) {
                if (message.startsWith("An HTTP line is larger than")) {
                    return HttpResponseStatus.REQUEST_URI_TOO_LONG.code();
                }
                if (message.startsWith("HTTP header is larger than")) {
                    return HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code();
                }
            }
        }
        return HttpResponseStatus.BAD_REQUEST.code();
    }

    private String formatAccessLogMessage(HttpServerRequest request, int statusCode) {
        String remoteHost = "-";
        SocketAddress remoteAddress = request.remoteAddress();
        if (remoteAddress != null) {
            remoteHost = remoteAddress.host();
        }

        String requestLine = request.method() + " " + request.uri() + " "
                + (request.version() != null ? request.version() : "HTTP/1.1");
        String timestamp = COMMON_LOG_DATE_FORMAT.format(ZonedDateTime.now());

        String effectivePattern = switch (pattern) {
            case "common" -> "%h %l %u %t \"%r\" %s %b";
            case "combined" -> "%h %l %u %t \"%r\" %s %b \"%{i,Referer}\" \"%{i,User-Agent}\"";
            default -> pattern;
        };

        return effectivePattern
                .replace("%h", remoteHost)
                .replace("%l", "-")
                .replace("%u", "-")
                .replace("%t", timestamp)
                .replace("%r", requestLine)
                .replace("%s", Integer.toString(statusCode))
                .replace("%b", "-")
                .replace("%D", "0")
                .replace("%{i,Referer}", "-")
                .replace("%{i,User-Agent}", "-");
    }
}
