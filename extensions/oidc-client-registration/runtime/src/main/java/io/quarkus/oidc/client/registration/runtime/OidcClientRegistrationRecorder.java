package io.quarkus.oidc.client.registration.runtime;

import static io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationsConfig.getDefaultClientRegistration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig.Metadata;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.oidc.common.runtime.OidcTlsSupport;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;

@Recorder
public class OidcClientRegistrationRecorder {
    private static final Logger LOG = Logger.getLogger(OidcClientRegistrationRecorder.class);
    private static final String DEFAULT_ID = "Default";

    private final RuntimeValue<OidcClientRegistrationsConfig> runtimeConfig;

    public OidcClientRegistrationRecorder(final RuntimeValue<OidcClientRegistrationsConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public OidcClientRegistrations setup(Supplier<Vertx> vertx, Supplier<TlsConfigurationRegistry> registrySupplier) {
        var tlsSupport = OidcTlsSupport.of(registrySupplier);
        OidcClientRegistration defaultClientReg = createOidcClientRegistration(
                getDefaultClientRegistration(runtimeConfig.getValue()),
                tlsSupport, vertx);

        Map<String, OidcClientRegistration> staticOidcClientRegs = new HashMap<>();
        for (var config : runtimeConfig.getValue().namedClientRegistrations().entrySet()) {
            staticOidcClientRegs.put(config.getKey(), createOidcClientRegistration(config.getValue(), tlsSupport, vertx));
        }

        return new OidcClientRegistrationsImpl(defaultClientReg, staticOidcClientRegs,
                new Function<OidcClientRegistrationConfig, Uni<OidcClientRegistration>>() {
                    @Override
                    public Uni<OidcClientRegistration> apply(OidcClientRegistrationConfig config) {
                        return createOidcClientRegistrationUni(config, tlsSupport, vertx);
                    }
                });
    }

    private static boolean isEmptyMetadata(Metadata m) {
        return m.clientName().isEmpty() && m.redirectUri().isEmpty()
                && m.postLogoutUri().isEmpty() && m.extraProps().isEmpty();
    }

    public Supplier<OidcClientRegistration> createOidcClientRegistrationBean(OidcClientRegistrations oidcClientRegs) {
        return new Supplier<OidcClientRegistration>() {

            @Override
            public OidcClientRegistration get() {
                return oidcClientRegs.getClientRegistration();
            }
        };
    }

    public Supplier<OidcClientRegistrations> createOidcClientRegistrationsBean(OidcClientRegistrations oidcClientRegs) {
        return new Supplier<OidcClientRegistrations>() {

            @Override
            public OidcClientRegistrations get() {
                return oidcClientRegs;
            }
        };
    }

    public static OidcClientRegistration createOidcClientRegistration(OidcClientRegistrationConfig oidcConfig,
            OidcTlsSupport tlsSupport, Supplier<Vertx> vertxSupplier) {
        return createOidcClientRegistrationUni(oidcConfig, tlsSupport, vertxSupplier).await()
                .atMost(oidcConfig.connectionTimeout());
    }

    public static Uni<OidcClientRegistration> createOidcClientRegistrationUni(OidcClientRegistrationConfig oidcConfig,
            OidcTlsSupport tlsSupport, Supplier<Vertx> vertxSupplier) {
        if (!oidcConfig.registrationEnabled()) {
            String message = String.format("'%s' client registration configuration is disabled", "");
            LOG.debug(message);
            return Uni.createFrom().item(new DisabledOidcClientRegistration(message));
        }

        try {
            if (oidcConfig.authServerUrl().isEmpty() && !OidcCommonUtils.isAbsoluteUrl(oidcConfig.registrationPath())) {
                if (isEmptyMetadata(oidcConfig.metadata())) {
                    return Uni.createFrom().nullItem();
                }
                var clientName = DEFAULT_ID.equals(oidcConfig.id().orElse(DEFAULT_ID)) ? "" : "." + oidcConfig.id().get();
                throw new ConfigurationException(
                        "Either 'quarkus.oidc-client-registration" + clientName
                                + ".auth-server-url' or absolute 'quarkus.oidc-client-registration" + clientName
                                + ".registration-path' URL must be set");
            }
            OidcCommonUtils.verifyEndpointUrl(getEndpointUrl(oidcConfig));
        } catch (Throwable t) {
            LOG.error(t.getMessage());
            String message = String.format("'%s' client registration configuration is not initialized",
                    oidcConfig.id().orElse("Default"));
            return Uni.createFrom().failure(new RuntimeException(message));
        }

        WebClientOptions options = new WebClientOptions();
        options.setFollowRedirects(oidcConfig.followRedirects());
        OidcCommonUtils.setHttpClientOptions(oidcConfig, options,
                tlsSupport.forConfig(oidcConfig.tls().tlsConfigurationName()));

        final io.vertx.mutiny.core.Vertx vertx = new io.vertx.mutiny.core.Vertx(vertxSupplier.get());
        WebClient client = WebClient.create(vertx, options);

        OidcFilterStorage oidcFilterStorage = OidcFilterStorage.get();
        Uni<OidcConfigurationMetadata> clientRegConfigUni = null;
        if (OidcCommonUtils.isAbsoluteUrl(oidcConfig.registrationPath())) {
            clientRegConfigUni = Uni.createFrom().item(
                    new OidcConfigurationMetadata(oidcConfig.registrationPath().get()));
        } else {
            String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);
            if (!oidcConfig.discoveryEnabled().orElse(true)) {
                clientRegConfigUni = Uni.createFrom()
                        .item(new OidcConfigurationMetadata(
                                OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.registrationPath())));
            } else {
                clientRegConfigUni = discoverRegistrationUri(client, authServerUriString, vertx, oidcConfig, oidcFilterStorage);
            }
        }
        return clientRegConfigUni.onItemOrFailure()
                .transformToUni(new BiFunction<OidcConfigurationMetadata, Throwable, Uni<? extends OidcClientRegistration>>() {

                    @Override
                    public Uni<OidcClientRegistration> apply(OidcConfigurationMetadata metadata, Throwable t) {
                        if (t != null) {
                            throw toOidcClientRegException(getEndpointUrl(oidcConfig), t);
                        }

                        if (metadata.clientRegistrationUri == null) {
                            throw new ConfigurationException(
                                    "OpenId Connect Provider client registration endpoint URL is not configured and can not be discovered");
                        }

                        final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);

                        ClientMetadata clientMetadata = OidcClientRegistrationImpl.createMetadata(oidcConfig.metadata());
                        if (!oidcConfig.registerEarly()) {
                            LOG.debugf("%s client registration is delayed",
                                    oidcConfig.id().orElse(DEFAULT_ID));
                            return Uni.createFrom().item(new OidcClientRegistrationImpl(client,
                                    connectionDelayInMillisecs,
                                    metadata.clientRegistrationUri,
                                    oidcConfig,
                                    null, oidcFilterStorage));
                        } else if (clientMetadata.getJsonObject().isEmpty()) {
                            LOG.debugf("%s client registration is skipped because its metadata is not configured",
                                    oidcConfig.id().orElse(DEFAULT_ID));
                            return Uni.createFrom().item(new OidcClientRegistrationImpl(client,
                                    connectionDelayInMillisecs,
                                    metadata.clientRegistrationUri,
                                    oidcConfig,
                                    null, oidcFilterStorage));
                        } else {
                            return OidcClientRegistrationImpl.registerClient(client, metadata.clientRegistrationUri,
                                    oidcConfig, clientMetadata.getMetadataString(), oidcFilterStorage)
                                    .onFailure(OidcCommonUtils.oidcEndpointNotAvailable())
                                    .retry()
                                    .withBackOff(OidcCommonUtils.CONNECTION_BACKOFF_DURATION,
                                            OidcCommonUtils.CONNECTION_BACKOFF_DURATION)
                                    .expireIn(connectionDelayInMillisecs)
                                    .onItemOrFailure()
                                    .transform(new BiFunction<RegisteredClient, Throwable, OidcClientRegistration>() {

                                        @Override
                                        public OidcClientRegistration apply(RegisteredClient r, Throwable t2) {
                                            RegisteredClient registeredClient;
                                            if (t2 != null) {
                                                LOG.errorf("%s client registration failed: %s, it can be retried later",
                                                        oidcConfig.id().orElse(DEFAULT_ID), t2.getMessage());
                                                registeredClient = null;
                                            } else {
                                                registeredClient = r;
                                                LOG.debugf("Registered client id: %s", r.metadata().getClientId());
                                            }
                                            return new OidcClientRegistrationImpl(client,
                                                    connectionDelayInMillisecs,
                                                    metadata.clientRegistrationUri,
                                                    oidcConfig,
                                                    registeredClient, oidcFilterStorage);
                                        }
                                    });
                        }
                    }
                });
    }

    private static String getEndpointUrl(OidcClientRegistrationConfig oidcConfig) {
        return oidcConfig.authServerUrl().isPresent() ? oidcConfig.authServerUrl().get() : oidcConfig.registrationPath().get();
    }

    private static Uni<OidcConfigurationMetadata> discoverRegistrationUri(WebClient client,
            String authServerUrl, io.vertx.mutiny.core.Vertx vertx, OidcClientRegistrationConfig oidcConfig,
            OidcFilterStorage oidcFilterStorage) {
        final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
        return OidcCommonUtils
                .discoverMetadata(client, new OidcRequestContextProperties(), authServerUrl, connectionDelayInMillisecs,
                        vertx, oidcConfig.useBlockingDnsLookup(), oidcFilterStorage)
                .onItem().transform(json -> new OidcConfigurationMetadata(json.getString("registration_endpoint")));
    }

    protected static OidcClientRegistrationException toOidcClientRegException(String authServerUrlString, Throwable cause) {
        return new OidcClientRegistrationException(OidcCommonUtils.formatConnectionErrorMessage(authServerUrlString), cause);
    }

    private static class DisabledOidcClientRegistration implements OidcClientRegistration {
        String message;

        DisabledOidcClientRegistration(String message) {
            this.message = message;
        }

        @Override
        public Uni<RegisteredClient> registeredClient() {
            throw new DisabledOidcClientRegistrationException(message);
        }

        @Override
        public Uni<RegisteredClient> registerClient(ClientMetadata reg) {
            throw new DisabledOidcClientRegistrationException(message);
        }

        @Override
        public Multi<RegisteredClient> registerClients(List<ClientMetadata> regs) {
            throw new DisabledOidcClientRegistrationException(message);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public Uni<RegisteredClient> readClient(String registrationUri, String registrationToken) {
            throw new DisabledOidcClientRegistrationException(message);
        }

    }

    private record OidcConfigurationMetadata(String clientRegistrationUri) {
    }

}
