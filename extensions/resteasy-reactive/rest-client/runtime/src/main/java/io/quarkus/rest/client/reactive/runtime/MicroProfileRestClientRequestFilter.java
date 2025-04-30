package io.quarkus.rest.client.reactive.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
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
            if (headerFiller instanceof ExtendedHeaderFiller) {
                ((ExtendedHeaderFiller) headerFiller).addHeaders(headers, requestContext);
            } else {
                headerFiller.addHeaders(headers);
            }

        }

        MultivaluedMap<String, String> incomingHeaders = determineIncomingHeaders();

        // Propagation with the default factory will then overwrite any values if required.
        for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
            requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
        }

        ClientHeadersFactory clientHeadersFactory = clientHeadersFactory(requestContext);
        if (clientHeadersFactory != null) {
            if (clientHeadersFactory instanceof ReactiveClientHeadersFactory reactiveClientHeadersFactory) {
                // reactive
                requestContext.suspend();
                reactiveClientHeadersFactory.getHeaders(incomingHeaders, headers).subscribe().with(
                        new Consumer<>() {
                            @Override
                            public void accept(MultivaluedMap<String, String> newHeaders) {
                                for (var headerEntry : newHeaders.entrySet()) {
                                    requestContext.getHeaders()
                                            .put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
                                }
                                requestContext.resume();
                            }
                        }, new Consumer<>() {
                            @Override
                            public void accept(Throwable t) {
                                requestContext.resume(t);
                            }
                        });
            } else {
                // blocking
                incomingHeaders = clientHeadersFactory.update(incomingHeaders, headers);
                for (var headerEntry : incomingHeaders.entrySet()) {
                    requestContext.getHeaders().put(headerEntry.getKey(), castToListOfObjects(headerEntry.getValue()));
                }
            }
        }
    }

    private MultivaluedMap<String, String> determineIncomingHeaders() {
        ArcContainer container = Arc.container();
        if (container == null) {
            return MicroProfileRestClientRequestFilter.EMPTY_MAP;
        }
        ManagedContext requestContext = container.requestContext();
        if (!requestContext.isActive()) {
            return MicroProfileRestClientRequestFilter.EMPTY_MAP;
        }
        InstanceHandle<HttpHeaders> jakartaRestServerHeaders = container.instance(HttpHeaders.class);
        if (!jakartaRestServerHeaders.isAvailable()) {
            return MicroProfileRestClientRequestFilter.EMPTY_MAP;
        }
        // TODO: we could in the future consider using the Vert.x request headers here as well...
        try {
            return jakartaRestServerHeaders.get().getRequestHeaders();
        } catch (ContextNotActiveException | IllegalStateException ignored) {
            // guard against the race condition that exists between checking if the context is active
            // and actually pulling the headers out of that request context
            // this could happen if the REST call is being offloaded to another thread pool in a fire and forget manner
            return MicroProfileRestClientRequestFilter.EMPTY_MAP;
        }
    }

    private ClientHeadersFactory clientHeadersFactory(ResteasyReactiveClientRequestContext requestContext) {
        if (requestContext.getConfiguration() instanceof ConfigurationImpl configuration) {
            ClientHeadersFactory localHeadersFactory = configuration.getFromContext(ClientHeadersFactory.class);
            if (localHeadersFactory != null) {
                return localHeadersFactory;
            }
        }

        return clientHeadersFactory;
    }

    private static List<String> castToListOfStrings(Collection<Object> values) {
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String) {
                result.add((String) value);
            } else if (value instanceof Collection) {
                result.addAll(castToListOfStrings((Collection<Object>) value));
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
