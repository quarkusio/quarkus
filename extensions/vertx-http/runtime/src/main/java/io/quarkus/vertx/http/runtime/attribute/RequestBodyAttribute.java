package io.quarkus.vertx.http.runtime.attribute;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.vertx.http.runtime.AccessLogConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.filters.accesslog.AccessLogBodySupport;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.ext.web.RoutingContext;

public class RequestBodyAttribute implements ExchangeAttribute {

    public static final String REQUEST_BODY = AccessLogBodySupport.REQUEST_BODY_TOKEN;

    private final boolean enabled;
    private final int maxSize;

    RequestBodyAttribute(boolean enabled, int maxSize) {
        this.enabled = enabled;
        this.maxSize = maxSize;
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        if (!enabled) {
            return null;
        }
        Object captured = exchange.get(AccessLogBodySupport.REQUEST_BODY_KEY);
        if (captured != null) {
            return captured.toString();
        }
        if (exchange.body() == null || exchange.body().length() == 0) {
            return null;
        }
        return AccessLogBodySupport.formatBody(exchange.body().buffer(), maxSize);
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Request body", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request body";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_BODY)) {
                AccessLogConfig config = getConfigMapping();
                return new RequestBodyAttribute(AccessLogBodySupport.isRequestBodyLoggingEnabled(config),
                        config.maxLoggedBodySize());
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }

        private static AccessLogConfig getConfigMapping() {
            SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
            return config.getConfigMapping(VertxHttpConfig.class).accessLog();
        }
    }
}
