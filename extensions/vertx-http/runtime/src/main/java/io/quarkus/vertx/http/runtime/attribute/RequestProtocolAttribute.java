package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;

/**
 * The request getProtocol
 *
 */
public class RequestProtocolAttribute implements ExchangeAttribute {

    public static final String REQUEST_PROTOCOL_SHORT = "%H";
    public static final String REQUEST_PROTOCOL = "%{PROTOCOL}";

    public static final ExchangeAttribute INSTANCE = new RequestProtocolAttribute();

    private RequestProtocolAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return getHttpVersionStr(exchange.request().version());
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Request getProtocol", newValue);
    }

    static String getHttpVersionStr(HttpVersion version) {
        // best effort to try and infer the HTTP version from
        // any "unknown" enum value
        return switch (version) {
            case HTTP_1_0 -> "HTTP/1.0";
            case HTTP_1_1 -> "HTTP/1.1";
            case HTTP_2 -> "HTTP/2";
            default ->
                // best effort to try and infer the HTTP version from
                // any "unknown" enum value
                version.name()
                        .replace("HTTP_", "HTTP/")
                        .replace("_", ".");
        };
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request getProtocol";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_PROTOCOL) || token.equals(REQUEST_PROTOCOL_SHORT)) {
                return RequestProtocolAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
