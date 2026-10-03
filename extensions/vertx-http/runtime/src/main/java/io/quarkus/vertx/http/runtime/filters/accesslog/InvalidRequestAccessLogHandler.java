package io.quarkus.vertx.http.runtime.filters.accesslog;

import java.util.Collections;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.netty.handler.codec.http.TooLongHttpLineException;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttribute;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeParser;
import io.quarkus.vertx.http.runtime.attribute.SubstituteEmptyWrapper;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * Logs invalid HTTP requests rejected by the Vert.x HTTP decoder (e.g. 414, 431) to the access log.
 * <p>
 * These requests never reach the Vert.x Web router, so the regular {@link AccessLogHandler} cannot log them.
 * Formatting uses the same {@link ExchangeAttribute} pipeline as the main access log.
 */
public class InvalidRequestAccessLogHandler implements Handler<HttpServerRequest> {

    private final AccessLogReceiver accessLogReceiver;
    private final ExchangeAttribute tokens;

    public InvalidRequestAccessLogHandler(AccessLogReceiver accessLogReceiver, String pattern, ClassLoader classLoader) {
        this.accessLogReceiver = accessLogReceiver;
        this.tokens = new ExchangeAttributeParser(classLoader, Collections.singletonList(new SubstituteEmptyWrapper("-")))
                .parse(AccessLogHandler.handleCommonNames(pattern));
    }

    @Override
    public void handle(HttpServerRequest request) {
        int statusCode = resolveStatusCode(request);
        request.response().setStatusCode(statusCode);
        accessLogReceiver.logMessage(tokens.readAttribute(new InvalidRequestRoutingContext(request)));
        HttpServerRequest.DEFAULT_INVALID_REQUEST_HANDLER.handle(request);
    }

    static int resolveStatusCode(HttpServerRequest request) {
        DecoderResult decoderResult = request.decoderResult();
        return statusCodeForCause(decoderResult != null ? decoderResult.cause() : null);
    }

    /**
     * Maps the decoder failure cause to an HTTP status code, mirroring
     * {@link HttpServerRequest#DEFAULT_INVALID_REQUEST_HANDLER} by matching Netty's typed exceptions
     * rather than their (unstable) messages. A test asserts this mapping so we notice if Netty changes it.
     */
    static int statusCodeForCause(Throwable cause) {
        if (cause instanceof TooLongHttpLineException) {
            return HttpResponseStatus.REQUEST_URI_TOO_LONG.code();
        }
        if (cause instanceof TooLongHttpHeaderException) {
            return HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code();
        }
        return HttpResponseStatus.BAD_REQUEST.code();
    }
}
