package io.quarkus.resteasy.reactive.client.microprofile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;

import io.quarkus.arc.Arc;
import io.smallrye.common.constraint.NotNull;
import io.smallrye.common.constraint.Nullable;

@Priority(Integer.MIN_VALUE)
public class MicroProfileRestRequestClientFilter implements ClientRequestFilter {
    private static final MultivaluedMap<String, String> EMPTY_MAP = new MultivaluedHashMap<>();

    @Nullable
    private final HeaderFiller headerFiller;
    @NotNull
    private final ClientHeadersFactory headersFactory;

    @Nullable
    private final Method method;

    /**
     *
     * @param headerFiller fills headers as specified in @ClientHeaderParam annotations
     * @param headersFactory MP Rest Client headersFactory
     * @param method java method of the JAX-RS interface
     */
    public MicroProfileRestRequestClientFilter(@Nullable HeaderFiller headerFiller,
            @NotNull ClientHeadersFactory headersFactory,
            // TODO: to optimize, add an option to disable passing the method?
            @Nullable Method method) {
        this.headerFiller = headerFiller;
        this.headersFactory = headersFactory;
        this.method = method;
    }

    // for each method, register one such filter
    @Override
    public void filter(ClientRequestContext requestContext) {
        // mutable collection of headers
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // gather original headers
        for (Map.Entry<String, List<Object>> headerEntry : requestContext.getHeaders().entrySet()) {
            headers.put(headerEntry.getKey(), castToListOfStrings(headerEntry.getValue()));
        }

        // add headers from MP annotations
        if (headerFiller != null) {
            // add headers to a mutable headers collection
            headerFiller.addHeaders(headers);
        }

        MultivaluedMap<String, String> incomingHeaders = MicroProfileRestRequestClientFilter.EMPTY_MAP;
        if (Arc.container().getActiveContext(RequestScoped.class) != null) {
            HeaderContainer headerContainer = Arc.container().instance(HeaderContainer.class).get();
            if (headerContainer != null) {
                incomingHeaders = headerContainer.getHeaders();
            }
        }

        if (headersFactory instanceof DefaultClientHeadersFactoryImpl) {
            // When using the default factory, pass the proposed outgoing headers onto the request context.
            // Propagation with the default factory will then overwrite any values if required.
            for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
                requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
            }
        }

        MultivaluedMap<String, String> updatedHeaders = headersFactory.update(incomingHeaders, headers);

        for (Map.Entry<String, List<String>> headerEntry : updatedHeaders.entrySet()) {
            requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
        }

        requestContext.setProperty("org.eclipse.microprofile.rest.client.invokedMethod", method);
    }

    private static List<String> castToListOfStrings(List<Object> values) {
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String) {
                result.add((String) value);
            } else {
                result.add(String.valueOf(value));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToListOfObjects(List<String> values) {
        return (List<Object>) (List<?>) values;
    }

}
