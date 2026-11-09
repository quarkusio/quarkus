package io.quarkus.vertx.http.runtime.filters.accesslog;

import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.vertx.http.runtime.attribute.ExchangeAttribute;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeParser;
import io.quarkus.vertx.http.runtime.attribute.SubstituteEmptyWrapper;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouterImpl;
import io.vertx.ext.web.impl.RoutingContextImpl;

/**
 * Logs invalid HTTP requests rejected by the Vert.x HTTP decoder (e.g. 414, 431) to the access log.
 * <p>
 * These requests never reach the Vert.x Web router, so the regular {@link AccessLogHandler} cannot log them.
 * Formatting uses the same {@link ExchangeAttribute} pipeline as the main access log.
 */
public class InvalidRequestAccessLogHandler implements Handler<HttpServerRequest> {

    private static final Logger LOGGER = Logger.getLogger(InvalidRequestAccessLogHandler.class);
    private static final RouterImpl DUMMY_ROUTER = new RouterImpl(null);

    private final AccessLogReceiver accessLogReceiver;
    private final ExchangeAttribute tokens;
    private final Pattern excludePattern;

    public InvalidRequestAccessLogHandler(AccessLogReceiver accessLogReceiver, String pattern, ClassLoader classLoader) {
        this(accessLogReceiver, pattern, classLoader, Optional.empty());
    }

    public InvalidRequestAccessLogHandler(AccessLogReceiver accessLogReceiver, String pattern, ClassLoader classLoader,
            Optional<String> excludePattern) {
        this.accessLogReceiver = accessLogReceiver;
        this.tokens = new ExchangeAttributeParser(classLoader, Collections.singletonList(new SubstituteEmptyWrapper("-")))
                .parse(AccessLogHandler.handleCommonNames(pattern));
        if (excludePattern.isPresent()) {
            this.excludePattern = Pattern.compile(excludePattern.get());
        } else {
            this.excludePattern = null;
        }
    }

    @Override
    public void handle(HttpServerRequest request) {
        HttpServerRequest.DEFAULT_INVALID_REQUEST_HANDLER.handle(request);
        try {
            RoutingContext rc = new RoutingContextImpl(null, DUMMY_ROUTER, request, Collections.emptySet());
            if (excludePattern != null) {
                String path = normalizedPathOrRaw(rc);
                if (path != null) {
                    Matcher m = excludePattern.matcher(path);
                    if (m.matches()) {
                        return;
                    }
                }
            }
            accessLogReceiver.logMessage(tokens.readAttribute(rc), rc);
        } catch (Throwable t) {
            LOGGER.debug("Failed to log invalid HTTP request", t);
        }
    }

    private static String normalizedPathOrRaw(RoutingContext rc) {
        try {
            return rc.normalizedPath();
        } catch (IllegalArgumentException e) {
            return rc.request().path();
        }
    }
}
