package org.jboss.resteasy.reactive.client.logging;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.ClientLogger;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public class DefaultClientLogger implements ClientLogger {
    private static final Logger log = Logger.getLogger(DefaultClientLogger.class);
    private static final String MASKED_VALUE = "<hidden>";

    private int bodySize;

    private Set<String> maskedHeaders = Collections.emptySet();

    @Override
    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    @Override
    public void setMaskedHeaders(Set<String> maskedHeaders) {
        this.maskedHeaders = maskedHeaders;
    }

    @Override
    public void logResponse(HttpClientResponse response, boolean redirect) {
        response.bodyHandler(new Handler<>() {
            @Override
            public void handle(Buffer body) {
                log.infof("%s: %s %s, Status[%d %s], Headers[%s], Body:\n%s",
                        redirect ? "Redirect" : "Response",
                        response.request().getMethod(), response.request().absoluteURI(), response.statusCode(),
                        response.statusMessage(), formatHeaders(response.headers()), bodyToString(body));
            }
        });
    }

    @Override
    public void logRequest(HttpClientRequest request, Buffer body, boolean omitBody) {
        if (omitBody) {
            log.infof("Request: %s %s Headers[%s], Body omitted",
                    request.getMethod(), request.absoluteURI(), formatHeaders(request.headers()));
        } else if (body == null || body.length() == 0) {
            log.infof("Request: %s %s Headers[%s], Empty body",
                    request.getMethod(), request.absoluteURI(), formatHeaders(request.headers()));
        } else {
            log.infof("Request: %s %s Headers[%s], Body:\n%s",
                    request.getMethod(), request.absoluteURI(), formatHeaders(request.headers()), bodyToString(body));
        }
    }

    private String bodyToString(Buffer body) {
        if (body == null) {
            return "";
        } else if (bodySize <= 0) {
            return body.toString();
        } else {
            String bodyAsString = body.toString();
            return bodyAsString.substring(0, Math.min(bodySize, bodyAsString.length()));
        }
    }

    private String formatHeaders(MultiMap headers) {
        if (headers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder((headers.size() * (6 + 1 + 6)) + (headers.size() - 1)); // this is a very rough estimate of a result like 'key1=value1 key2=value2'

        for (Map.Entry<String, String> entry : headers) {
            boolean shouldBeMasked = this.maskedHeaders.contains(entry.getKey().toLowerCase(Locale.ROOT));

            String value = shouldBeMasked
                    ? MASKED_VALUE
                    : entry.getValue();

            sb.append(entry.getKey()).append('=').append(value).append(' ');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
