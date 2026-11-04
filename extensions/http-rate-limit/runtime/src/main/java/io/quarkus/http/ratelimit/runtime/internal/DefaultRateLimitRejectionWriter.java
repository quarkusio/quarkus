package io.quarkus.http.ratelimit.runtime.internal;

import jakarta.inject.Singleton;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Answers 429 with {@code Too Many Requests} as plain text. A {@code HEAD} request gets no body.
 * <p>
 * The body says only that the client sent too many requests: it never names the rule or the key, which are internal
 * configuration and may identify the client. {@code Retry-After} is already on the response when {@link #write} is
 * called. The connection stays open.
 */
@Singleton
public class DefaultRateLimitRejectionWriter {

    private static final String TEXT_BODY = "Too Many Requests";

    public void write(RoutingContext context) {
        HttpServerResponse response = context.response();
        response.setStatusCode(HttpResponseStatus.TOO_MANY_REQUESTS.code());
        if (context.request().method() == HttpMethod.HEAD) {
            response.end();
            return;
        }
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
        response.end(TEXT_BODY);
    }
}
