package io.quarkus.http.ratelimit.runtime.internal;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.http.ratelimit.RateLimitRejectionInfo;
import io.quarkus.http.ratelimit.RateLimitRejectionWriter;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;

/**
 * Answers 429 or 503 with an RFC 9457 problem when the client accepts JSON ({@code application/problem+json},
 * {@code application/json} or {@code text/json}, in the type it asks for), and with plain text in every other case,
 * including a request without an {@code Accept} header or one that accepts any type. A {@code HEAD} request gets no
 * body.
 * <p>
 * The body says only that the client sent too many requests or the service is unavailable: it never names the rule or the key,
 * which are internal
 * configuration and may identify the client. {@code Retry-After} is already on the response when {@link #write} is
 * called. The connection stays open.
 */
@Singleton
@Reserve
@Priority(0)
public class DefaultRateLimitRejectionWriter implements RateLimitRejectionWriter {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_JSON = "text/json";
    private static final String TEXT_PLAIN = "text/plain";

    private static final String JSON_BODY = "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429}";
    private static final String TEXT_BODY = "Too Many Requests";
    private static final String UNAVAILABLE_JSON_BODY = "{\"type\":\"about:blank\",\"title\":\"Service Unavailable\",\"status\":503}";
    private static final String UNAVAILABLE_TEXT_BODY = "Service Unavailable";

    // the order decides wildcards: */* and text/* pick text/plain, application/* picks the problem type
    private static final List<MIMEHeader> SUPPORTED = List.of(mime(TEXT_PLAIN), mime(PROBLEM_JSON),
            mime(APPLICATION_JSON), mime(TEXT_JSON));

    @Override
    public void write(RoutingContext context, RateLimitRejectionInfo rejection) {
        HttpServerResponse response = context.response();
        response.setStatusCode(rejection.statusCode());
        boolean unavailable = rejection.statusCode() == HttpResponseStatus.SERVICE_UNAVAILABLE.code();
        if (context.request().method() == HttpMethod.HEAD) {
            response.end();
            return;
        }
        String contentType = contentType(context);
        if (TEXT_PLAIN.equals(contentType)) {
            response.putHeader(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN + "; charset=utf-8");
            response.end(unavailable ? UNAVAILABLE_TEXT_BODY : TEXT_BODY);
        } else {
            response.putHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.end(unavailable ? UNAVAILABLE_JSON_BODY : JSON_BODY);
        }
    }

    /**
     * @return the supported type the client accepts best, or text when it accepts none of them
     */
    private static String contentType(RoutingContext context) {
        MIMEHeader best = context.parsedHeaders().findBestUserAcceptedIn(context.parsedHeaders().accept(), SUPPORTED);
        return best == null ? TEXT_PLAIN : best.value();
    }

    private static MIMEHeader mime(String type) {
        return new ParsableMIMEValue(type).forceParse();
    }
}
