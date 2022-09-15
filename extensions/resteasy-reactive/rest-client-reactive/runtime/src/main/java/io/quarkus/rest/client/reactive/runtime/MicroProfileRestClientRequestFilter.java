package io.quarkus.rest.client.reactive.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.HeaderFiller;
import io.quarkus.rest.client.reactive.ReactiveClientHeadersFactory;

@Priority(Integer.MIN_VALUE)
public class MicroProfileRestClientRequestFilter implements ResteasyReactiveClientRequestFilter {

    private static final MultivaluedMap<String, String> EMPTY_MAP = new MultivaluedHashMap<>();

    private final ClientHeadersFactory clientHeadersFactory;

    public MicroProfileRestClientRequestFilter(ClientHeadersFactory clientHeadersFactory) {
        this.clientHeadersFactory = clientHeadersFactory;
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        HeaderFiller headerFiller = (HeaderFiller) requestContext.getProperty(HeaderFiller.class.getName());

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

        MultivaluedMap<String, String> incomingHeaders = MicroProfileRestClientRequestFilter.EMPTY_MAP;
        if (Arc.container().getActiveContext(RequestScoped.class) != null) {
            HeaderContainer headerContainer = Arc.container().instance(HeaderContainer.class).get();
            if (headerContainer != null) {
                incomingHeaders = headerContainer.getHeaders();
            }
        }

        if (clientHeadersFactory instanceof DefaultClientHeadersFactoryImpl) {
            // When using the default factory, pass the proposed outgoing headers onto the request context.
            // Propagation with the default factory will then overwrite any values if required.
            for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
                requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
            }
        }

        if (clientHeadersFactory != null) {
            if (clientHeadersFactory instanceof ReactiveClientHeadersFactory) {
                // reactive
                ReactiveClientHeadersFactory reactiveClientHeadersFactory = (ReactiveClientHeadersFactory) clientHeadersFactory;
                requestContext.suspend();
                reactiveClientHeadersFactory.getHeaders(incomingHeaders, headers).subscribe().with(newHeaders -> {
                    for (Map.Entry<String, List<String>> headerEntry : newHeaders.entrySet()) {
                        requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
                    }
                    requestContext.resume();
                }, requestContext::resume);
            } else {
                // blocking
                incomingHeaders = clientHeadersFactory.update(incomingHeaders, headers);

                for (Map.Entry<String, List<String>> headerEntry : incomingHeaders.entrySet()) {
                    requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
                }
            }
        } else {
            for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
                requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
            }
        }
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
