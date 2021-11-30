package io.quarkus.rest.client.reactive;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import io.smallrye.mutiny.Uni;

/**
 * Reactive ClientHeadersFactory flavor for Quarkus rest-client reactive extension.
 */
public abstract class ReactiveClientHeadersFactory implements ClientHeadersFactory {
    public abstract Uni<MultivaluedMap<String, String>> getHeaders(MultivaluedMap<String, String> incomingHeaders);

    @Override
    public final MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        throw new RuntimeException(
                "Can't call `update` method in a Reactive context. Use `getHeaders` or implement ClientHeadersFactory.");
    }
}
