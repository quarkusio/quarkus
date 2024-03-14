package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import io.smallrye.mutiny.Uni;

/**
 * Reactive ClientHeadersFactory flavor for Quarkus rest-client reactive extension.
 */
public abstract class ReactiveClientHeadersFactory implements ClientHeadersFactory {

    /**
     * Updates the HTTP headers to send to the remote service. Note that providers
     * on the outbound processing chain could further update the headers.
     *
     * @param incomingHeaders the map of headers from the inbound JAX-RS request. This will be an empty map if the
     *        associated client interface is not part of a JAX-RS request.
     * @param clientOutgoingHeaders the read-only map of header parameters specified on the client interface.
     * @return a Uni with a map of HTTP headers to merge with the clientOutgoingHeaders to be sent to the remote service.
     *
     * @see ClientHeadersFactory#update(MultivaluedMap, MultivaluedMap)
     */
    public abstract Uni<MultivaluedMap<String, String>> getHeaders(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders);

    @Override
    public final MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        throw new RuntimeException(
                "Can't call `update` method in a Reactive context. Use `getHeaders` or implement ClientHeadersFactory.");
    }
}
