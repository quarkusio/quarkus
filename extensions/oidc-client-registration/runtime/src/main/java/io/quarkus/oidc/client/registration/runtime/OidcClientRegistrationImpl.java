package io.quarkus.oidc.client.registration.runtime;

import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.json.JsonObjectBuilder;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig.Metadata;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcRequestFilter.OidcRequestContext;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniOnItem;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcClientRegistrationImpl implements OidcClientRegistration {
    private static final Logger LOG = Logger.getLogger(OidcClientRegistrationImpl.class);
    private static final String APPLICATION_JSON = "application/json";
    private static final String AUTHORIZATION_HEADER = String.valueOf(HttpHeaders.AUTHORIZATION);
    private static final String DEFAULT_ID = "Default";

    private final WebClient client;
    private final long connectionDelayInMillisecs;
    private final String registrationUri;
    private final OidcClientRegistrationConfig oidcConfig;
    private final RegisteredClient registeredClient;
    private final OidcFilterStorage oidcFilterStorage;
    private volatile boolean closed;

    OidcClientRegistrationImpl(WebClient client, long connectionDelayInMillisecs,
            String registrationUri, OidcClientRegistrationConfig oidcConfig, RegisteredClient registeredClient,
            OidcFilterStorage oidcFilterStorage) {
        this.client = client;
        this.connectionDelayInMillisecs = connectionDelayInMillisecs;
        this.registrationUri = registrationUri;
        this.oidcConfig = oidcConfig;
        this.registeredClient = registeredClient;
        this.oidcFilterStorage = oidcFilterStorage;
    }

    @Override
    public Uni<RegisteredClient> registeredClient() {
        if (registeredClient != null) {
            return Uni.createFrom().item(registeredClient);
        } else if (oidcConfig.registerEarly()) {
            return Uni.createFrom().nullItem();
        } else {
            ClientMetadata metadata = createMetadata(oidcConfig.metadata());
            if (metadata.getJsonObject().isEmpty()) {
                LOG.debugf("%s client registration is skipped because its metadata is not configured",
                        oidcConfig.id().orElse(DEFAULT_ID));
                return Uni.createFrom().nullItem();
            } else {
                return registerClient(client, registrationUri, oidcConfig, metadata.getMetadataString(), oidcFilterStorage)
                        .onFailure(OidcCommonUtils.oidcEndpointNotAvailable())
                        .retry()
                        .withBackOff(OidcCommonUtils.CONNECTION_BACKOFF_DURATION,
                                OidcCommonUtils.CONNECTION_BACKOFF_DURATION)
                        .expireIn(connectionDelayInMillisecs);
            }
        }
    }

    @Override
    public Uni<RegisteredClient> registerClient(ClientMetadata metadata) {
        LOG.debugf("Register client metadata: %s", metadata.getMetadataString());
        checkClosed();
        OidcRequestContextProperties requestProps = getRequestProps();
        return postRequest(requestProps, client, registrationUri, oidcConfig, metadata.getMetadataString(), oidcFilterStorage)
                .transform(resp -> newRegisteredClient(resp, client, oidcConfig, requestProps, oidcFilterStorage));
    }

    @Override
    public Multi<RegisteredClient> registerClients(List<ClientMetadata> metadataList) {
        LOG.debugf("Register clients");
        checkClosed();
        OidcRequestContextProperties requestProps = getRequestProps();
        return Multi.createFrom().emitter(new Consumer<MultiEmitter<? super RegisteredClient>>() {
            @Override
            public void accept(MultiEmitter<? super RegisteredClient> multiEmitter) {
                try {
                    AtomicInteger emitted = new AtomicInteger();
                    for (ClientMetadata metadata : metadataList) {
                        postRequest(requestProps, client, registrationUri, oidcConfig,
                                metadata.getMetadataString(), oidcFilterStorage)
                                .transform(
                                        resp -> newRegisteredClient(resp, client, oidcConfig, requestProps, oidcFilterStorage))
                                .subscribe().with(new Consumer<RegisteredClient>() {
                                    @Override
                                    public void accept(RegisteredClient client) {
                                        multiEmitter.emit(client);
                                        if (emitted.incrementAndGet() == metadataList.size()) {
                                            multiEmitter.complete();
                                        }
                                    }
                                });
                    }
                } catch (Exception ex) {
                    multiEmitter.fail(ex);
                }
            }
        });
    }

    private OidcRequestContextProperties getRequestProps() {
        return oidcFilterStorage.isEmpty() ? null : new OidcRequestContextProperties();
    }

    static Uni<RegisteredClient> registerClient(WebClient client,
            String registrationUri,
            OidcClientRegistrationConfig oidcConfig,
            String clientRegJson, OidcFilterStorage oidcFilterStorage) {
        OidcRequestContextProperties requestProps = oidcFilterStorage.isEmpty() ? null
                : new OidcRequestContextProperties();
        return postRequest(requestProps, client, registrationUri, oidcConfig, clientRegJson, oidcFilterStorage)
                .transform(resp -> newRegisteredClient(resp, client, oidcConfig, requestProps, oidcFilterStorage));
    }

    static UniOnItem<HttpResponse<Buffer>> postRequest(OidcRequestContextProperties requestProps,
            WebClient client, String registrationUri,
            OidcClientRegistrationConfig oidcConfig, String clientRegJson, OidcFilterStorage oidcFilterStorage) {
        HttpRequest<Buffer> request = client.postAbs(registrationUri);
        request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), APPLICATION_JSON);
        request.putHeader(HttpHeaders.ACCEPT.toString(), APPLICATION_JSON);
        if (oidcConfig.initialToken().orElse(null) != null) {
            request.putHeader(AUTHORIZATION_HEADER, OidcConstants.BEARER_SCHEME + " " + oidcConfig.initialToken().get());
        }
        // Retry up to three times with a one-second delay between the retries if the connection is closed
        Buffer buffer = Buffer.buffer(clientRegJson);
        Uni<HttpResponse<Buffer>> response = filterHttpRequest(requestProps, request, buffer, oidcFilterStorage)
                .sendBuffer(OidcCommonUtils.getRequestBuffer(requestProps, buffer))
                .onFailure(SocketException.class)
                .retry()
                .atMost(oidcConfig.connectionRetryCount())
                .onFailure().transform(t -> {
                    LOG.warn("OIDC Server is not available:", t.getCause() != null ? t.getCause() : t);
                    // don't wrap it to avoid information leak
                    return new OidcClientRegistrationException("OIDC Server is not available");
                });
        return response.onItem();
    }

    private static HttpRequest<Buffer> filterHttpRequest(OidcRequestContextProperties requestProps,
            HttpRequest<Buffer> request, Buffer body, OidcFilterStorage oidcFilterStorage) {
        if (!oidcFilterStorage.isEmpty()) {
            OidcRequestContext context = new OidcRequestContext(request, body, requestProps);
            for (OidcRequestFilter filter : oidcFilterStorage.getOidcRequestFilters(Type.CLIENT_REGISTRATION, context)) {
                filter.filter(context);
            }
        }
        return request;
    }

    static private RegisteredClient newRegisteredClient(HttpResponse<Buffer> resp,
            WebClient client, OidcClientRegistrationConfig oidcConfig,
            OidcRequestContextProperties requestProps, OidcFilterStorage oidcFilterStorage) {
        Buffer buffer = OidcCommonUtils.filterHttpResponse(requestProps, resp,
                OidcEndpoint.Type.CLIENT_REGISTRATION, oidcFilterStorage);
        if (resp.statusCode() == 200 || resp.statusCode() == 201) {
            JsonObject json = buffer.toJsonObject();
            LOG.debugf("Client has been succesfully registered: %s", json.toString());

            String registrationClientUri = (String) json.remove(OidcConstants.REGISTRATION_CLIENT_URI);
            String registrationToken = (String) json.remove(OidcConstants.REGISTRATION_ACCESS_TOKEN);

            ClientMetadata metadata = new ClientMetadata(json.toString());

            return new RegisteredClientImpl(client, oidcConfig, metadata, registrationClientUri, registrationToken,
                    oidcFilterStorage);
        } else {
            String errorMessage = buffer.toString();
            LOG.errorf("Client registeration has failed:  status: %d, error message: %s", resp.statusCode(),
                    errorMessage);
            throw new OidcClientRegistrationException(errorMessage);
        }
    }

    @Override
    public Uni<RegisteredClient> readClient(String registrationUri, String registrationToken) {
        @SuppressWarnings("resource")
        RegisteredClient newClient = new RegisteredClientImpl(client, oidcConfig, createMetadata(oidcConfig.metadata()),
                registrationUri, registrationToken, oidcFilterStorage);
        return newClient.read();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                client.close();
            } catch (Exception ex) {
                LOG.debug("Failed to close the client", ex);
            }
            closed = true;
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("OIDC Client Registration is closed");
        }
    }

    static class ClientRegistrationHelper {
        RegisteredClient client;
        String registrationUri;

        ClientRegistrationHelper(RegisteredClient client, String registrationUri) {
            this.client = client;
            this.registrationUri = registrationUri;
        }
    }

    static ClientMetadata createMetadata(Metadata metadata) {
        JsonObjectBuilder json = jsonProvider().createObjectBuilder();
        if (metadata.clientName().isPresent()) {
            json.add(OidcConstants.CLIENT_METADATA_CLIENT_NAME, metadata.clientName().get());
        }
        if (metadata.redirectUri().isPresent()) {
            json.add(OidcConstants.CLIENT_METADATA_REDIRECT_URIS,
                    jsonProvider().createArrayBuilder().add(metadata.redirectUri().get()));
        }
        if (metadata.postLogoutUri().isPresent()) {
            json.add(OidcConstants.POST_LOGOUT_REDIRECT_URI,
                    jsonProvider().createArrayBuilder().add(metadata.postLogoutUri().get()));
        }
        for (Map.Entry<String, String> entry : metadata.extraProps().entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }

        return new ClientMetadata(json.build());
    }
}
