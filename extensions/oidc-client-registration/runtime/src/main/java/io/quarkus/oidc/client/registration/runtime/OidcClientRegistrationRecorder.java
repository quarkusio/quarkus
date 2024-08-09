package io.quarkus.oidc.client.registration.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig.Metadata;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfiguration;
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

    public OidcClientRegistrations setup(OidcClientRegistrationsConfig oidcClientRegsConfig,
            Supplier<Vertx> vertx, Supplier<TlsConfigurationRegistry> registrySupplier) {
        var defaultTlsConfiguration = registrySupplier.get().getDefault().orElse(null);

        OidcClientRegistration defaultClientReg = createOidcClientRegistration(oidcClientRegsConfig.defaultClientRegistration,
                defaultTlsConfiguration, vertx);

        Map<String, OidcClientRegistration> staticOidcClientRegs = new HashMap<>();

        for (Map.Entry<String, OidcClientRegistrationConfig> config : oidcClientRegsConfig.namedClientRegistrations
                .entrySet()) {
            staticOidcClientRegs.put(config.getKey(),
                    createOidcClientRegistration(config.getValue(), defaultTlsConfiguration, vertx));
        }

        return new OidcClientRegistrationsImpl(defaultClientReg, staticOidcClientRegs,
                new Function<OidcClientRegistrationConfig, Uni<OidcClientRegistration>>() {
                    @Override
                    public Uni<OidcClientRegistration> apply(OidcClientRegistrationConfig config) {
                        return createOidcClientRegistrationUni(config, defaultTlsConfiguration, vertx);
                    }
                });
    }

    private static boolean isEmptyMetadata(Metadata m) {
        return m.clientName.isEmpty() && m.redirectUri.isEmpty()
                && m.postLogoutUri.isEmpty() && m.extraProps.isEmpty();
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
            TlsConfiguration tlsConfig, Supplier<Vertx> vertxSupplier) {
        return createOidcClientRegistrationUni(oidcConfig, tlsConfig, vertxSupplier).await()
                .atMost(oidcConfig.connectionTimeout);
    }

    public static Uni<OidcClientRegistration> createOidcClientRegistrationUni(OidcClientRegistrationConfig oidcConfig,
            TlsConfiguration tlsConfig, Supplier<Vertx> vertxSupplier) {
        if (!oidcConfig.registrationEnabled) {
            String message = String.format("'%s' client registration configuration is disabled", "");
            LOG.debug(message);
            return Uni.createFrom().item(new DisabledOidcClientRegistration(message));
        }

        try {
            if (oidcConfig.authServerUrl.isEmpty() && !OidcCommonUtils.isAbsoluteUrl(oidcConfig.registrationPath)) {
                if (isEmptyMetadata(oidcConfig.metadata)) {
                    return Uni.createFrom().nullItem();
                }
                throw new ConfigurationException(
                        "Either 'quarkus.oidc-client-registration.auth-server-url' or absolute 'quarkus.oidc-client-registration.registration-path' URL must be set");
            }
            OidcCommonUtils.verifyEndpointUrl(getEndpointUrl(oidcConfig));
        } catch (Throwable t) {
            LOG.error(t.getMessage());
            String message = String.format("'%s' client registration configuration is not initialized",
                    oidcConfig.id.orElse("Default"));
            return Uni.createFrom().failure(new RuntimeException(message));
        }

        WebClientOptions options = new WebClientOptions();

        OidcCommonUtils.setHttpClientOptions(oidcConfig, options, tlsConfig);

        final io.vertx.mutiny.core.Vertx vertx = new io.vertx.mutiny.core.Vertx(vertxSupplier.get());
        WebClient client = WebClient.create(vertx, options);

        Map<OidcEndpoint.Type, List<OidcRequestFilter>> oidcRequestFilters = OidcCommonUtils.getOidcRequestFilters();

        Uni<OidcConfigurationMetadata> tokenUrisUni = null;
        if (OidcCommonUtils.isAbsoluteUrl(oidcConfig.registrationPath)) {
            tokenUrisUni = Uni.createFrom().item(
                    new OidcConfigurationMetadata(oidcConfig.registrationPath.get()));
        } else {
            String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);
            if (!oidcConfig.getDiscoveryEnabled().orElse(true)) {
                tokenUrisUni = Uni.createFrom()
                        .item(new OidcConfigurationMetadata(
                                OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.registrationPath)));
            } else {
                tokenUrisUni = discoverRegistrationUri(client, oidcRequestFilters, authServerUriString.toString(), vertx,
                        oidcConfig);
            }
        }
        return tokenUrisUni.onItemOrFailure()
                .transformToUni(new BiFunction<OidcConfigurationMetadata, Throwable, Uni<? extends OidcClientRegistration>>() {

                    @Override
                    public Uni<OidcClientRegistration> apply(OidcConfigurationMetadata metadata, Throwable t) {
                        if (t != null) {
                            throw toOidcClientRegException(getEndpointUrl(oidcConfig), t);
                        }

                        if (metadata.tokenRegistrationUri == null) {
                            throw new ConfigurationException(
                                    "OpenId Connect Provider registration endpoint URL is not configured and can not be discovered");
                        }

                        ClientMetadata clientMetadata = createMetadata(oidcConfig.metadata);
                        if (clientMetadata.getJsonObject().isEmpty()) {
                            LOG.debugf("%s client registartion is skipped because its metadata is not configured",
                                    oidcConfig.id.orElse(DEFAULT_ID));
                            return Uni.createFrom().item(new OidcClientRegistrationImpl(client,
                                    metadata.tokenRegistrationUri,
                                    oidcConfig,
                                    null,
                                    oidcRequestFilters));
                        } else {
                            final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
                            return OidcClientRegistrationImpl.registerClient(client,
                                    metadata.tokenRegistrationUri,
                                    oidcConfig, oidcRequestFilters, clientMetadata.getMetadataString())
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
                                                LOG.errorf("%s client registartion failed: %s, it can be retried later",
                                                        oidcConfig.id.orElse(DEFAULT_ID), t2.getMessage());
                                                registeredClient = null;
                                            } else {
                                                registeredClient = r;
                                            }
                                            return new OidcClientRegistrationImpl(client,
                                                    metadata.tokenRegistrationUri,
                                                    oidcConfig,
                                                    registeredClient,
                                                    oidcRequestFilters);
                                        }
                                    });
                        }
                    }
                });
    }

    private static String getEndpointUrl(OidcClientRegistrationConfig oidcConfig) {
        return oidcConfig.authServerUrl.isPresent() ? oidcConfig.authServerUrl.get() : oidcConfig.registrationPath.get();
    }

    private static Uni<OidcConfigurationMetadata> discoverRegistrationUri(WebClient client,
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> oidcRequestFilters,
            String authServerUrl, io.vertx.mutiny.core.Vertx vertx, OidcClientRegistrationConfig oidcConfig) {
        final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
        return OidcCommonUtils
                .discoverMetadata(client, oidcRequestFilters, new OidcRequestContextProperties(), authServerUrl,
                        connectionDelayInMillisecs, vertx,
                        oidcConfig.useBlockingDnsLookup)
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
        public RegisteredClient registeredClient() {
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

    }

    private static class OidcConfigurationMetadata {
        private final String tokenRegistrationUri;

        OidcConfigurationMetadata(String tokenRegistrationUri) {
            this.tokenRegistrationUri = tokenRegistrationUri;
        }
    }

    private static ClientMetadata createMetadata(Metadata metadata) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        if (metadata.clientName.isPresent()) {
            json.add(OidcConstants.CLIENT_METADATA_CLIENT_NAME, metadata.clientName.get());
        }
        if (metadata.redirectUri.isPresent()) {
            json.add(OidcConstants.CLIENT_METADATA_REDIRECT_URIS,
                    Json.createArrayBuilder().add(metadata.redirectUri.get()));
        }
        if (metadata.postLogoutUri.isPresent()) {
            json.add(OidcConstants.POST_LOGOUT_REDIRECT_URI,
                    Json.createArrayBuilder().add(metadata.postLogoutUri.get()));
        }
        for (Map.Entry<String, String> entry : metadata.extraProps.entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }

        return new ClientMetadata(json.build());
    }

}
