package io.quarkus.vertx.http.runtime;

import static org.jboss.logging.Logger.getLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.core.Handler;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;

public class QuarkusErrorHandler implements Handler<RoutingContext> {

    private static final Logger log = getLogger(QuarkusErrorHandler.class);

    /**
     * we don't want to generate a new UUID each time as it is slowish. Instead we just generate one based one
     * and then use a counter.
     */
    private static final String BASE_ID = UUID.randomUUID().toString() + "-";

    private static final AtomicLong ERROR_COUNT = new AtomicLong();

    private final boolean showStack;
    private final Optional<HttpConfiguration.PayloadHint> contentTypeDefault;

    public QuarkusErrorHandler(boolean showStack, Optional<HttpConfiguration.PayloadHint> contentTypeDefault) {
        this.showStack = showStack;
        this.contentTypeDefault = contentTypeDefault;
    }

    @Override
    public void handle(RoutingContext event) {
        try {
            if (event.failure() == null) {
                event.response().setStatusCode(event.statusCode());
                event.response().end();
                return;
            }
            //this can happen if there is no auth mechanisms
            if (event.failure() instanceof UnauthorizedException) {
                HttpAuthenticator authenticator = event.get(HttpAuthenticator.class.getName());
                if (authenticator != null) {
                    authenticator.sendChallenge(event).subscribe().with(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) {
                            event.response().end();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            event.fail(throwable);
                        }
                    });
                } else {
                    event.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code()).end();
                }
                return;
            }
            if (event.failure() instanceof ForbiddenException) {
                event.response().setStatusCode(HttpResponseStatus.FORBIDDEN.code()).end();
                return;
            }
            if (event.failure() instanceof AuthenticationFailedException) {
                //generally this should be handled elsewhere
                //but if we get to this point bad things have happened
                //so it is better to send a response than to hang
                event.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code()).end();
                return;
            }
        } catch (IllegalStateException e) {
            //ignore this if the response is already started
            if (!event.response().ended()) {
                //could be that just the head is committed
                event.response().end();
            }
            return;
        }

        if (!event.response().headWritten()) {
            event.response().setStatusCode(event.statusCode() > 0 ? event.statusCode() : 500);
        }

        String uuid = BASE_ID + ERROR_COUNT.incrementAndGet();
        String details;
        String stack = "";
        Throwable exception = event.failure();
        String responseContentType = null;
        try {
            responseContentType = ContentTypes.pickFirstSupportedAndAcceptedContentType(event);
        } catch (RuntimeException e) {
            // Let's shield ourselves from bugs in this parsing code:
            // we're already handling an exception,
            // so the priority is to return *something* to the user.
            // If we can't pick the appropriate content-type, well, so be it.
            exception.addSuppressed(e);
        }
        if (showStack && exception != null) {
            details = generateHeaderMessage(exception, uuid);
            stack = generateStackTrace(exception);
        } else {
            details = generateHeaderMessage(uuid);
        }
        if (event.failure() instanceof IOException) {
            log.debugf(exception,
                    "IOError processing HTTP request to %s failed, the client likely terminated the connection. Error id: %s",
                    event.request().uri(), uuid);
        } else {
            log.errorf(exception, "HTTP Request to %s failed, error id: %s", event.request().uri(), uuid);
        }
        //we have logged the error
        //now lets see if we can actually send a response
        //if not we just return
        if (event.response().ended()) {
            return;
        } else if (event.response().headWritten()) {
            event.response().end();
            return;
        }

        if (responseContentType == null) {
            responseContentType = "";
        }
        switch (responseContentType) {
            case ContentTypes.TEXT_HTML:
            case ContentTypes.APPLICATION_XHTML:
            case ContentTypes.APPLICATION_XML:
            case ContentTypes.TEXT_XML:
                htmlResponse(event, details, exception);
                break;
            case ContentTypes.APPLICATION_JSON:
            case ContentTypes.TEXT_JSON:
                jsonResponse(event, responseContentType, details, stack);
                break;
            default:
                // We default to JSON representation
                switch (contentTypeDefault.orElse(HttpConfiguration.PayloadHint.JSON)) {
                    case HTML:
                        htmlResponse(event, details, exception);
                        break;
                    case JSON:
                    default:
                        jsonResponse(event, ContentTypes.APPLICATION_JSON, details, stack);
                        break;
                }
                break;
        }
    }

    private void jsonResponse(RoutingContext event, String contentType, String details, String stack) {
        event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=utf-8");
        String escapedDetails = escapeJsonString(details);
        String escapedStack = escapeJsonString(stack);
        StringBuilder jsonPayload = new StringBuilder("{\"details\":\"")
                .append(escapedDetails)
                .append("\",\"stack\":\"")
                .append(escapedStack)
                .append("\"}");
        writeResponse(event, jsonPayload.toString());
    }

    private void htmlResponse(RoutingContext event, String details, Throwable exception) {
        event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        final TemplateHtmlBuilder htmlBuilder = new TemplateHtmlBuilder("Internal Server Error", details, details);
        if (showStack && exception != null) {
            htmlBuilder.stack(exception);
        }
        writeResponse(event, htmlBuilder.toString());
    }

    private void writeResponse(RoutingContext event, String output) {
        if (!event.response().ended()) {
            event.response().end(output);
        }
    }

    private static String generateStackTrace(final Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        return stringWriter.toString().trim();
    }

    private static String generateHeaderMessage(final Throwable exception, String uuid) {
        return String.format("Error id %s, %s: %s", uuid, exception.getClass().getName(),
                extractFirstLine(exception.getMessage()));
    }

    private static String generateHeaderMessage(String uuid) {
        return String.format("Error id %s", uuid);
    }

    private static String extractFirstLine(final String message) {
        if (null == message) {
            return "";
        }

        String[] lines = message.split("\\r?\\n");
        return lines[0].trim();
    }

    static String escapeJsonString(final String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static final class ContentTypes {

        private ContentTypes() {
        }

        private static final String APPLICATION_JSON = "application/json";
        private static final String TEXT_JSON = "text/json";
        private static final String TEXT_HTML = "text/html";
        private static final String APPLICATION_XHTML = "application/xhtml+xml";
        private static final String APPLICATION_XML = "application/xml";
        private static final String TEXT_XML = "text/xml";

        // WARNING: The order matters for wildcards: if text/json is before text/html, then text/* will match text/json.
        private static final Collection<MIMEHeader> SUPPORTED = Arrays.asList(
                new ParsableMIMEValue(APPLICATION_JSON).forceParse(),
                new ParsableMIMEValue(TEXT_JSON).forceParse(),
                new ParsableMIMEValue(TEXT_HTML).forceParse(),
                new ParsableMIMEValue(APPLICATION_XHTML).forceParse(),
                new ParsableMIMEValue(APPLICATION_XML).forceParse(),
                new ParsableMIMEValue(TEXT_XML).forceParse());

        static String pickFirstSupportedAndAcceptedContentType(RoutingContext context) {
            List<MIMEHeader> acceptableTypes = context.parsedHeaders().accept();
            MIMEHeader result = context.parsedHeaders().findBestUserAcceptedIn(acceptableTypes, SUPPORTED);
            return result == null ? null : result.value();
        }
    }
}
