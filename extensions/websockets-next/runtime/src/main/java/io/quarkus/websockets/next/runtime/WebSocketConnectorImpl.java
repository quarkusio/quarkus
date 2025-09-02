package io.quarkus.websockets.next.runtime;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.runtime.WebSocketClientRecorder.ClientEndpoint;
import io.quarkus.websockets.next.runtime.WebSocketClientRecorder.ClientEndpointsContext;
import io.quarkus.websockets.next.runtime.config.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.runtime.telemetry.SendingInterceptor;
import io.quarkus.websockets.next.runtime.telemetry.WebSocketTelemetryProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.impl.VertxImpl;

@Typed(WebSocketConnector.class)
@Dependent
public class WebSocketConnectorImpl<CLIENT> extends WebSocketConnectorBase<WebSocketConnectorImpl<CLIENT>>
        implements WebSocketConnector<CLIENT> {

    // derived properties

    private final ClientEndpoint clientEndpoint;

    private final WebSocketTelemetryProvider telemetryProvider;

    WebSocketConnectorImpl(InjectionPoint injectionPoint, Codecs codecs, Vertx vertx, ClientConnectionManager connectionManager,
            ClientEndpointsContext endpointsContext, WebSocketsClientRuntimeConfig config,
            TlsConfigurationRegistry tlsConfigurationRegistry, Instance<WebSocketTelemetryProvider> telemetryProvider) {
        super(vertx, codecs, connectionManager, config, tlsConfigurationRegistry);
        this.clientEndpoint = Objects.requireNonNull(endpointsContext.endpoint(getEndpointClass(injectionPoint)));
        this.telemetryProvider = telemetryProvider.isResolvable() ? telemetryProvider.get() : null;
        setPath(clientEndpoint.path);
    }

    @Override
    public Uni<WebSocketClientConnection> connect() {
        // A new client is created for each connection
        // The client is created when the returned Uni is subscribed
        // The client is closed when the connection is closed
        AtomicReference<WebSocketClient> client = new AtomicReference<>();

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

        var telemetrySupport = telemetryProvider == null ? null
                : telemetryProvider.createClientTelemetrySupport(clientEndpoint.path);
        Uni<WebSocketOpen> websocketOpen = Uni.createFrom().<WebSocketOpen> emitter(e -> {
            // Create a new event loop context for each client, otherwise the current context is used
            // We want to avoid a situation where if multiple clients/connections are created in a row,
            // the same event loop is used and so writing/receiving messages is de-facto serialized
            // Get rid of this workaround once https://github.com/eclipse-vertx/vert.x/issues/5366 is resolved
            ContextImpl context = ((VertxImpl) vertx).createEventLoopContext();
            context.dispatch(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    try {
                        WebSocketClient c = vertx.createWebSocketClient(populateClientOptions());
                        client.setPlain(c);
                        if (telemetrySupport != null && telemetrySupport.interceptConnection()) {
                            telemetrySupport.connectionOpened();
                        }
                        c.connect(connectOptions, new Handler<AsyncResult<WebSocket>>() {
                            @Override
                            public void handle(AsyncResult<WebSocket> r) {
                                if (r.succeeded()) {
                                    e.complete(new WebSocketOpen(newCleanupConsumer(c, context), r.result()));
                                } else {
                                    if (telemetrySupport != null && telemetrySupport.interceptConnection()) {
                                        telemetrySupport.connectionOpeningFailed(r.cause());
                                    }
                                    e.fail(r.cause());
                                }
                            }
                        });
                    } catch (RuntimeException re) {
                        e.fail(re);
                    }
                }
            });
        });
        return websocketOpen.map(wsOpen -> {
            WebSocket ws = wsOpen.websocket();
            TrafficLogger trafficLogger = TrafficLogger.forClient(config);
            SendingInterceptor sendingInterceptor = telemetrySupport == null ? null : telemetrySupport.getSendingInterceptor();
            WebSocketClientConnectionImpl connection = new WebSocketClientConnectionImpl(clientEndpoint.clientId,
                    ws,
                    codecs,
                    pathParams,
                    serverEndpointUri,
                    headers,
                    trafficLogger,
                    userData,
                    sendingInterceptor,
                    wsOpen.cleanup());
            if (trafficLogger != null) {
                trafficLogger.connectionOpened(connection);
            }
            connectionManager.add(clientEndpoint.generatedEndpointClass, connection);

            Endpoints.initialize(vertx, Arc.container(), codecs, connection, ws,
                    clientEndpoint.generatedEndpointClass, config.autoPingInterval(), SecuritySupport.NOOP,
                    config.unhandledFailureStrategy(), trafficLogger,
                    () -> {
                        connectionManager.remove(clientEndpoint.generatedEndpointClass, connection);
                        client.get().close();
                    }, true, true, telemetrySupport);

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
