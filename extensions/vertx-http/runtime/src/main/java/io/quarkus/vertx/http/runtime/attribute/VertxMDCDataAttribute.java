package io.quarkus.vertx.http.runtime.attribute;

import io.quarkus.vertx.core.runtime.VertxMDC;
import io.vertx.ext.web.RoutingContext;

/**
 * Provide entries from the MDC section of the RoutingContext. This is especially helpful to put OTel data 'traceId' and
 * 'spanId' into the access log.
 */
public class VertxMDCDataAttribute implements ExchangeAttribute {

    private final String dataKey;

    public VertxMDCDataAttribute(String dataKey) {
        this.dataKey = dataKey;
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        VertxMDC mdc = VertxMDC.INSTANCE;
        return mdc.get(dataKey);
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "OTel data";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.startsWith("%{X,") && token.endsWith("}")) {
                final String dataItemName = token.substring(4, token.length() - 1);
                return new VertxMDCDataAttribute(dataItemName);
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }

}
