package io.quarkus.vertx.http.runtime;

import static org.jboss.logging.Logger.getLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QuarkusErrorHandler implements Handler<RoutingContext> {

    private static final Logger log = getLogger(QuarkusErrorHandler.class);

    /**
     * we don't want to generate a new UUID each time as it is slowish. Instead we just generate one based one
     * and then use a counter.
     */
    private static final String BASE_ID = UUID.randomUUID().toString() + "-";

    private static final AtomicLong ERROR_COUNT = new AtomicLong();

    private final boolean showStack;

    public QuarkusErrorHandler(boolean showStack) {
        this.showStack = showStack;
    }

    @Override
    public void handle(RoutingContext event) {
        if (event.failure() == null) {
            event.response().setStatusCode(event.statusCode());
            event.response().end();
            return;
        }
        event.response().setStatusCode(500);
        String uuid = BASE_ID + ERROR_COUNT.incrementAndGet();
        String details = "";
        String stack = "";
        Throwable exception = event.failure();
        if (showStack && exception != null) {
            details = generateHeaderMessage(exception, uuid);
            stack = generateStackTrace(exception);

        } else {
            details += "Error id " + uuid;
        }
        if (event.failure() instanceof IOException) {
            log.debugf(exception,
                    "IOError processing HTTP request to %s failed, the client likely terminated the connection. Error id: %s",
                    event.request().uri(), uuid);
        } else {
            log.errorf(exception, "HTTP Request to %s failed, error id: %s", event.request().uri(), uuid);
        }
        String accept = event.request().getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
            String escapedStack = stack.replace(System.lineSeparator(), "\\n").replace("\"", "\\\"");
            StringBuilder jsonPayload = new StringBuilder("{\"details\":\"").append(details).append("\",\"stack\":\"")
                    .append(escapedStack).append("\"}");
            event.response().end(jsonPayload.toString());
        } else {
            //We default to HTML representation
            event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
            final TemplateHtmlBuilder htmlBuilder = new TemplateHtmlBuilder("Internal Server Error", details, details);
            if (showStack && exception != null) {
                htmlBuilder.stack(exception);
            }
            event.response().end(htmlBuilder.toString());
        }
    }

    private static String generateStackTrace(final Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        return escapeHtml(stringWriter.toString().trim());
    }

    private static String generateHeaderMessage(final Throwable exception, String uuid) {
        return escapeHtml(String.format("Error handling %s, %s: %s", uuid, exception.getClass().getName(),
                extractFirstLine(exception.getMessage())));
    }

    private static String extractFirstLine(final String message) {
        if (null == message) {
            return "";
        }

        String[] lines = message.split("\\r?\\n");
        return lines[0].trim();
    }

    private static String escapeHtml(final String bodyText) {
        if (bodyText == null) {
            return "null";
        }

        return bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

}
