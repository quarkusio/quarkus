package io.quarkus.vertx.http.runtime;

import static org.jboss.logging.Logger.getLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.runtime.ErrorPageAction;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.runtime.logging.DecorateStackUtil;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.core.Handler;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;

public class QuarkusErrorHandler implements Handler<RoutingContext> {

    private static final Logger log = getLogger(QuarkusErrorHandler.class);
    private static final String NL = "\n";
    private static final String TAB = "\t";
    private static final String HEADING = "500 - Internal Server Error";

    /**
     * we don't want to generate a new UUID each time as it is slowish. Instead, we just generate one based one
     * and then use a counter.
     */
    private static final String BASE_ID = UUID.randomUUID() + "-";

    private static final AtomicLong ERROR_COUNT = new AtomicLong();

    private final boolean showStack;
    private final boolean decorateStack;
    private final Optional<HttpConfiguration.PayloadHint> contentTypeDefault;
    private final List<ErrorPageAction> actions;
    private final List<String> knowClasses;
    private final String srcMainJava;

    public QuarkusErrorHandler(boolean showStack, boolean decorateStack,
            Optional<HttpConfiguration.PayloadHint> contentTypeDefault) {
        this(showStack, decorateStack, contentTypeDefault, null, List.of(), List.of());
    }

    public QuarkusErrorHandler(boolean showStack, boolean decorateStack,
            Optional<HttpConfiguration.PayloadHint> contentTypeDefault,
            String srcMainJava,
            List<String> knowClasses,
            List<ErrorPageAction> actions) {
        this.showStack = showStack;
        this.decorateStack = decorateStack;
        this.contentTypeDefault = contentTypeDefault;
        this.srcMainJava = srcMainJava;
        this.knowClasses = knowClasses;
        this.actions = actions;
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
                    authenticator.sendChallenge(event).subscribe().with(new Consumer<>() {
                        @Override
                        public void accept(Boolean aBoolean) {
                            event.response().end();
                        }
                    }, new Consumer<>() {
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

            if (event.failure() instanceof AuthenticationException) {
                if (event.response().getStatusCode() == HttpResponseStatus.OK.code()) {
                    //set 401 if status wasn't set upstream
                    event.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
                }

                //when proactive security is enabled and this wasn't handled elsewhere, we expect event to
                //end here as failing event makes it possible to customize response, however when proactive security is
                //disabled, this should be handled elsewhere and if we get to this point bad things have happened,
                //so it is better to send a response than to hang

                if (event.failure() instanceof AuthenticationCompletionException
                        && event.failure().getMessage() != null
                        && LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                    event.response().end(event.failure().getMessage());
                } else {
                    event.response().end();
                }
                return;
            }

            if (event.failure() instanceof RejectedExecutionException) {
                log.warn(
                        "Worker thread pool exhaustion, no more worker threads available - returning a `503 - SERVICE UNAVAILABLE` response.");
                event.response().setStatusCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()).end();
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
        //now let's see if we can actually send a response
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
                jsonResponse(event, responseContentType, details, stack, exception);
                break;
            case ContentTypes.TEXT_PLAIN:
                textResponse(event, details, stack, exception);
                break;
            default:
                if (contentTypeDefault.isPresent()) {
                    switch (contentTypeDefault.get()) {
                        case HTML:
                            htmlResponse(event, details, exception);
                            break;
                        case JSON:
                            jsonResponse(event, ContentTypes.APPLICATION_JSON, details, stack, exception);
                            break;
                        case TEXT:
                            textResponse(event, details, stack, exception);
                            break;
                        default:
                            defaultResponse(event, details, stack, exception);
                            break;
                    }
                } else {
                    defaultResponse(event, details, stack, exception);
                    break;
                }
                break;
        }
    }

    private void defaultResponse(RoutingContext event, String details, String stack, Throwable throwable) {
        String userAgent = event.request().getHeader("User-Agent");
        if (userAgent != null && (userAgent.toLowerCase(Locale.ROOT).startsWith("wget/")
                || userAgent.toLowerCase(Locale.ROOT).startsWith("curl/"))) {
            textResponse(event, details, stack, throwable);
        } else {
            jsonResponse(event, ContentTypes.APPLICATION_JSON, details, stack, throwable);
        }
    }

    private void textResponse(RoutingContext event, String details, String stack, Throwable throwable) {
        event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TEXT_PLAIN + "; charset=utf-8");
        String decoratedString = null;
        if (decorateStack && throwable != null) {
            decoratedString = DecorateStackUtil.getDecoratedString(throwable, srcMainJava, knowClasses);
        }

        try (StringWriter sw = new StringWriter()) {
            sw.write(NL + HEADING + NL);
            sw.write("---------------------------" + NL);
            sw.write(NL);
            sw.write("Details:");
            sw.write(NL);
            sw.write(TAB + details);
            sw.write(NL);
            if (decoratedString != null) {
                sw.write("Decorate (Source code):");
                sw.write(NL);
                sw.write(TAB + decoratedString);
                sw.write(NL);
            }
            sw.write("Stack:");
            sw.write(NL);
            sw.write(TAB + stack);
            sw.write(NL);
            writeResponse(event, sw.toString());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void jsonResponse(RoutingContext event, String contentType, String details, String stack, Throwable throwable) {
        event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=utf-8");
        String escapedDetails = escapeJsonString(details);
        String escapedStack = escapeJsonString(stack);
        String decoratedString = null;
        if (decorateStack && throwable != null) {
            decoratedString = DecorateStackUtil.getDecoratedString(throwable, srcMainJava, knowClasses);
        }

        StringBuilder jsonPayload = new StringBuilder("{\"details\":\"")
                .append(escapedDetails);

        if (decoratedString != null) {
            jsonPayload = jsonPayload.append("\",\"decorate\":\"")
                    .append(escapeJsonString(decoratedString));
        }

        jsonPayload = jsonPayload.append("\",\"stack\":\"")
                .append(escapedStack)
                .append("\"}");
        writeResponse(event, jsonPayload.toString());
    }

    private void htmlResponse(RoutingContext event, String details, Throwable exception) {
        event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        final TemplateHtmlBuilder htmlBuilder = new TemplateHtmlBuilder(showStack, "Internal Server Error", details, details,
                this.actions);

        if (decorateStack && exception != null) {
            htmlBuilder.decorate(exception, this.srcMainJava, this.knowClasses);
        }
        if (showStack && exception != null) {
            htmlBuilder.stack(exception, this.knowClasses);
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
        private static final String TEXT_PLAIN = "text/plain";
        private static final String APPLICATION_XHTML = "application/xhtml+xml";
        private static final String APPLICATION_XML = "application/xml";
        private static final String TEXT_XML = "text/xml";

        // WARNING: The order matters for wildcards: if text/json is before text/html, then text/* will match text/json.
        private static final List<MIMEHeader> BASE_HEADERS = List.of(
                createParsableMIMEValue(APPLICATION_JSON),
                createParsableMIMEValue(TEXT_JSON),
                createParsableMIMEValue(TEXT_HTML),
                createParsableMIMEValue(APPLICATION_XHTML),
                createParsableMIMEValue(APPLICATION_XML),
                createParsableMIMEValue(TEXT_XML));

        private static final Collection<MIMEHeader> SUPPORTED = createSupported();
        private static final Collection<MIMEHeader> SUPPORTED_CURL = createSupportedCurl();

        private static Collection<MIMEHeader> createSupported() {
            var supported = new ArrayList<MIMEHeader>(BASE_HEADERS.size() + 1);
            supported.addAll(BASE_HEADERS);
            supported.add(createParsableMIMEValue(TEXT_PLAIN));
            return Collections.unmodifiableCollection(supported);
        }

        private static Collection<MIMEHeader> createSupportedCurl() {
            var supportedCurl = new ArrayList<MIMEHeader>(BASE_HEADERS.size() + 1);
            supportedCurl.add(createParsableMIMEValue(TEXT_PLAIN));
            supportedCurl.addAll(BASE_HEADERS);
            return Collections.unmodifiableCollection(supportedCurl);
        }

        private static ParsableMIMEValue createParsableMIMEValue(String applicationJson) {
            return new ParsableMIMEValue(applicationJson).forceParse();
        }

        static String pickFirstSupportedAndAcceptedContentType(RoutingContext context) {
            List<MIMEHeader> acceptableTypes = context.parsedHeaders().accept();

            String userAgent = context.request().getHeader("User-Agent");
            if (userAgent != null && (userAgent.toLowerCase(Locale.ROOT).startsWith("wget/")
                    || userAgent.toLowerCase(Locale.ROOT).startsWith("curl/"))) {
                MIMEHeader result = context.parsedHeaders().findBestUserAcceptedIn(acceptableTypes, SUPPORTED_CURL);
                return result == null ? null : result.value();
            } else {
                MIMEHeader result = context.parsedHeaders().findBestUserAcceptedIn(acceptableTypes, SUPPORTED);
                return result == null ? null : result.value();
            }
        }
    }
}
