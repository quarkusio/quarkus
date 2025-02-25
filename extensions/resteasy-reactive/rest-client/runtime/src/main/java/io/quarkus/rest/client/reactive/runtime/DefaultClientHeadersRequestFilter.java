package io.quarkus.rest.client.reactive.runtime;

import java.util.List;
import java.util.function.BiConsumer;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

@Priority(Integer.MIN_VALUE + 10)
public class DefaultClientHeadersRequestFilter implements ClientRequestFilter {
    private final MultivaluedMap<String, Object> defaultHeaders;

    public DefaultClientHeadersRequestFilter(final MultivaluedMap<String, Object> defaultHeaders) {
        this.defaultHeaders = new CaseInsensitiveMap<>();
        this.defaultHeaders.putAll(defaultHeaders);
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        this.defaultHeaders.forEach(new BiConsumer<String, List<Object>>() {
            @Override
            public void accept(final String name, final List<Object> values) {
                requestContext.getHeaders().addAll(name, values);
            }
        });
    }
}
