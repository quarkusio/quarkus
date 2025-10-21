package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.vertx.http.runtime.AccessLogConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class AllRequestHeadersAttribute implements ExchangeAttribute {

    private static final String AUTHORIZATION_HEADER = String.valueOf(HttpHeaders.AUTHORIZATION).toLowerCase();
    private static final String COOKIE_HEADER = String.valueOf(HttpHeaders.COOKIE).toLowerCase();

    private final Set<String> maskedHeaders;
    private final Set<String> maskedCookies;

    AllRequestHeadersAttribute() {
        this(Set.of(), Set.of());
    }

    AllRequestHeadersAttribute(AccessLogConfig config) {
        this(config.maskedHeaders().orElse(Set.of()), config.maskedCookies().orElse(Set.of()));
    }

    AllRequestHeadersAttribute(Set<String> maskedHeaders, Set<String> maskedCookies) {
        this.maskedHeaders = toLowerCaseStringSet(maskedHeaders);
        this.maskedCookies = toLowerCaseStringSet(maskedCookies);
    }

    private static Set<String> toLowerCaseStringSet(Set<String> set) {
        return set.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        return readAttribute(exchange.request().headers());
    }

    String readAttribute(MultiMap headers) {
        if (headers.isEmpty()) {
            return null;
        } else {
            final StringJoiner joiner = new StringJoiner(System.lineSeparator());

            for (Map.Entry<String, String> header : headers) {
                joiner.add(header.getKey() + ": " + maskHeaderValue(header.getKey(), header.getValue()));
            }

            return joiner.toString();
        }
    }

    String maskHeaderValue(String headerName, String headerValue) {
        if (headerValue == null) {
            return null;
        }

        String headerNameLowerCase = headerName.toLowerCase();

        if (AUTHORIZATION_HEADER.equals(headerNameLowerCase)) {
            return maskAuthorizationHeaderValue(headerValue);
        }

        if (COOKIE_HEADER.equals(headerNameLowerCase)) {
            return maskCookieHeaderValue(headerValue);
        }

        if (maskedHeaders.contains(headerNameLowerCase)) {
            return "...";
        }

        return headerValue;
    }

    private String maskAuthorizationHeaderValue(String headerValue) {
        int idx = headerValue.indexOf(' ');
        final String scheme = idx > 0 ? headerValue.substring(0, idx) : null;

        if (scheme != null) {
            return scheme + " ...";
        } else {
            return "...";
        }
    }

    private String maskCookieHeaderValue(String headerValue) {
        int idx = headerValue.indexOf('=');

        final String cookieName = idx > 0 ? headerValue.substring(0, idx) : null;

        if (cookieName != null && maskedCookies.contains(cookieName.toLowerCase())) {
            return cookieName + "=...";
        }

        return headerValue;
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Headers", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Headers";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals("%{ALL_REQUEST_HEADERS}")) {
                return new AllRequestHeadersAttribute(getConfigMapping());
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
