package io.quarkus.websockets.next.runtime;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.runtime.WebSocketClientRecorder.ClientEndpoint;
import io.quarkus.websockets.next.runtime.WebSocketClientRecorder.ClientEndpointsContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

@Typed(WebSocketConnector.class)
@Dependent
public class WebSocketConnectorImpl<CLIENT> extends WebSocketConnectorBase<WebSocketConnectorImpl<CLIENT>>
        implements WebSocketConnector<CLIENT> {

    // derived properties

    private final ClientEndpoint clientEndpoint;

    WebSocketConnectorImpl(InjectionPoint injectionPoint, Codecs codecs, Vertx vertx, ClientConnectionManager connectionManager,
            ClientEndpointsContext endpointsContext, WebSocketsClientRuntimeConfig config,
            TlsConfigurationRegistry tlsConfigurationRegistry) {
        super(vertx, codecs, connectionManager, config, tlsConfigurationRegistry);
        this.clientEndpoint = Objects.requireNonNull(endpointsContext.endpoint(getEndpointClass(injectionPoint)));
        setPath(clientEndpoint.path);
    }

    @Override
    public Uni<WebSocketClientConnection> connect() {
        // Currently we create a new client for each connection
        // The client is closed when the connection is closed
        // TODO would it make sense to share clients?
        WebSocketClient client = vertx.createWebSocketClient(populateClientOptions());

        StringBuilder serverEndpoint = new StringBuilder();
        if (baseUri != null) {
            serverEndpoint.append(baseUri.toString());
        } else {
            // Obtain the base URI from the config
            String key = clientEndpoint.clientId + ".base-uri";
            Optional<String> maybeBaseUri = ConfigProvider.getConfig().getOptionalValue(key, String.class);
            if (maybeBaseUri.isEmpty()) {
                throw new WebSocketClientException("Unable to obtain the config value for: " + key);
            }
            serverEndpoint.append(maybeBaseUri.get());
        }
        serverEndpoint.append(replacePathParameters(clientEndpoint.path));

        URI serverEndpointUri;
        try {
            serverEndpointUri = new URI(serverEndpoint.toString());
        } catch (URISyntaxException e) {
            throw new WebSocketClientException(e);
        }

        WebSocketConnectOptions connectOptions = newConnectOptions(serverEndpointUri);
        StringBuilder uri = new StringBuilder();
        if (serverEndpointUri.getPath() != null) {
            uri.append(serverEndpointUri.getRawPath());
        }
        if (serverEndpointUri.getQuery() != null) {
            uri.append("?").append(serverEndpointUri.getQuery());
        }
        connectOptions.setURI(uri.toString());
        for (Entry<String, List<String>> e : headers.entrySet()) {
            for (String val : e.getValue()) {
                connectOptions.addHeader(e.getKey(), val);
            }
        }
        subprotocols.forEach(connectOptions::addSubProtocol);

        return Uni.createFrom().completionStage(() -> client.connect(connectOptions).toCompletionStage())
                .map(ws -> {
                    TrafficLogger trafficLogger = TrafficLogger.forClient(config);
                    WebSocketClientConnectionImpl connection = new WebSocketClientConnectionImpl(clientEndpoint.clientId, ws,
                            codecs,
                            pathParams,
                            serverEndpointUri, headers, trafficLogger);
                    if (trafficLogger != null) {
                        trafficLogger.connectionOpened(connection);
                    }
                    connectionManager.add(clientEndpoint.generatedEndpointClass, connection);

                    Endpoints.initialize(vertx, Arc.container(), codecs, connection, ws,
                            clientEndpoint.generatedEndpointClass, config.autoPingInterval(), SecuritySupport.NOOP,
                            config.unhandledFailureStrategy(), trafficLogger,
                            () -> {
                                connectionManager.remove(clientEndpoint.generatedEndpointClass, connection);
                                client.close();
                            }, true);

                    return connection;
                });
    }

    String getEndpointClass(InjectionPoint injectionPoint) {
        // The type is validated during build - if it does not represent a client endpoint the build fails
        // WebSocketConnectorImpl<org.acme.Foo> -> org.acme.Foo
        ParameterizedType parameterizedType = (ParameterizedType) injectionPoint.getType();
        return parameterizedType.getActualTypeArguments()[0].getTypeName();
    }

}
