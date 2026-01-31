package io.quarkus.rest.client.reactive;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import io.vertx.core.http.HttpServerRequest;

/**
 * A {@link ClientHeadersFactory} implementation that propagates HTTP headers from the incoming request
 * to outgoing REST client requests.
 * <p>
 * This factory reads the
 * {@code org.eclipse.microprofile.rest.client.propagateHeaders} configuration property to determine
 * which headers should be propagated from the incoming HTTP request to the REST client calls.
 * <p>
 * The propagated headers are extracted from the {@link HttpServerRequest} and added to the outgoing
 * request headers of the REST client.
 */
@Provider
public class VertxRequestClientHeadersFactory implements ClientHeadersFactory {

    private static final String PROPAGATE_PROPERTY = "org.eclipse.microprofile.rest.client.propagateHeaders";

    private final HttpServerRequest httpServerRequest;

    public VertxRequestClientHeadersFactory(HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
    }

    private static Optional<Config> config() {
        try {
            return Optional.ofNullable(ConfigProvider.getConfig());
        } catch (NoClassDefFoundError | IllegalStateException | ExceptionInInitializerError var1) {
            return Optional.empty();
        }
    }

    private static Optional<String> getHeadersProperty() {
        Optional<Config> c = config();
        return c
                .flatMap(config -> config.getOptionalValue(PROPAGATE_PROPERTY, String.class));
    }

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> ignore,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> propagatedHeaders = new MultivaluedHashMap<>();
        Optional<String> optionalPropagateHeaders = getHeadersProperty();

        optionalPropagateHeaders.ifPresent(s -> Arrays.stream(s.split(","))
                .forEach(header -> {
                    String httpServerRequestHeader = httpServerRequest.getHeader(header);
                    if (httpServerRequestHeader != null) {
                        List<String> headers = Arrays.stream(httpServerRequestHeader.split(","))
                                .toList();
                        propagatedHeaders.put(header, headers);
                    }
                }));

        return propagatedHeaders;
    }
}
