package io.quarkus.oidc.client.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientConfig.Grant;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.ext.web.client.WebClient;

@Recorder
public class OidcClientRecorder {

    private static final Logger LOG = Logger.getLogger(OidcClientRecorder.class);
    private static final String DEFAULT_OIDC_CLIENT_ID = "Default";

    public OidcClients setup(OidcClientsConfig oidcClientsConfig, TlsConfig tlsConfig, Supplier<Vertx> vertx) {

        String defaultClientId = oidcClientsConfig.defaultClient.getId().orElse(DEFAULT_OIDC_CLIENT_ID);
        OidcClient defaultClient = createOidcClient(oidcClientsConfig.defaultClient, defaultClientId, tlsConfig, vertx);

        Map<String, OidcClient> staticOidcClients = new HashMap<>();

        for (Map.Entry<String, OidcClientConfig> config : oidcClientsConfig.namedClients.entrySet()) {
            OidcCommonUtils.verifyConfigurationId(defaultClientId, config.getKey(), config.getValue().getId());
            staticOidcClients.put(config.getKey(),
                    createOidcClient(config.getValue(), config.getKey(), tlsConfig, vertx));
        }

        return new OidcClientsImpl(defaultClient, staticOidcClients,
                new Function<OidcClientConfig, Uni<OidcClient>>() {
                    @Override
                    public Uni<OidcClient> apply(OidcClientConfig config) {
                        return createOidcClientUni(config, config.getId().get(), tlsConfig, vertx);
                    }
                });
    }

    public Supplier<OidcClient> createOidcClientBean(OidcClients clients) {
        return new Supplier<OidcClient>() {

            @Override
            public OidcClient get() {
                return clients.getClient();
            }
        };
    }

    public Supplier<OidcClient> createOidcClientBean(OidcClients clients, String clientName) {
        return new Supplier<OidcClient>() {

            @Override
            public OidcClient get() {
                return clients.getClient(clientName);
            }
        };
    }

    public Supplier<OidcClients> createOidcClientsBean(OidcClients clients) {
        return new Supplier<OidcClients>() {

            @Override
            public OidcClients get() {
                return clients;
            }
        };
    }

    protected static OidcClient createOidcClient(OidcClientConfig oidcConfig, String oidcClientId,
            TlsConfig tlsConfig, Supplier<Vertx> vertx) {
        return createOidcClientUni(oidcConfig, oidcClientId, tlsConfig, vertx).await().atMost(oidcConfig.connectionTimeout);
    }

    protected static Uni<OidcClient> createOidcClientUni(OidcClientConfig oidcConfig, String oidcClientId,
            TlsConfig tlsConfig, Supplier<Vertx> vertx) {
        if (!oidcConfig.isClientEnabled()) {
            String message = String.format("'%s' client configuration is disabled", oidcClientId);
            LOG.debug(message);
            return Uni.createFrom().item(new DisabledOidcClient(message));
        }
        if (!oidcConfig.getId().isPresent()) {
            oidcConfig.setId(oidcClientId);
        }

        try {
            if (oidcConfig.authServerUrl.isEmpty() && !OidcCommonUtils.isAbsoluteUrl(oidcConfig.tokenPath)) {
                throw new ConfigurationException(
                        "Either 'quarkus.oidc-client.auth-server-url' or absolute 'quarkus.oidc-client.token-path' URL must be set");
            }
            OidcCommonUtils.verifyEndpointUrl(getEndpointUrl(oidcConfig));
            OidcCommonUtils.verifyCommonConfiguration(oidcConfig, false, false);
        } catch (Throwable t) {
            LOG.debug(t.getMessage());
            String message = String.format("'%s' client configuration is not initialized", oidcClientId);
            return Uni.createFrom().item(new DisabledOidcClient(message));
        }

        WebClientOptions options = new WebClientOptions();

        OidcCommonUtils.setHttpClientOptions(oidcConfig, tlsConfig, options);

        var mutinyVertx = new io.vertx.mutiny.core.Vertx(vertx.get());
        WebClient client = WebClient.create(mutinyVertx, options);

        Map<OidcEndpoint.Type, List<OidcRequestFilter>> oidcRequestFilters = OidcCommonUtils.getOidcRequestFilters();

        Uni<OidcConfigurationMetadata> tokenUrisUni = null;
        if (OidcCommonUtils.isAbsoluteUrl(oidcConfig.tokenPath)) {
            tokenUrisUni = Uni.createFrom().item(
                    new OidcConfigurationMetadata(oidcConfig.tokenPath.get(),
                            OidcCommonUtils.isAbsoluteUrl(oidcConfig.revokePath) ? oidcConfig.revokePath.get() : null));
        } else {
            String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);
            if (!oidcConfig.discoveryEnabled.orElse(true)) {
                tokenUrisUni = Uni.createFrom()
                        .item(new OidcConfigurationMetadata(
                                OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.tokenPath),
                                OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.revokePath)));
            } else {
                tokenUrisUni = discoverTokenUris(client, oidcRequestFilters, authServerUriString.toString(), oidcConfig,
                        mutinyVertx);
            }
        }
        return tokenUrisUni.onItemOrFailure()
                .transform(new BiFunction<OidcConfigurationMetadata, Throwable, OidcClient>() {

                    @Override
                    public OidcClient apply(OidcConfigurationMetadata metadata, Throwable t) {
                        if (t != null) {
                            throw toOidcClientException(getEndpointUrl(oidcConfig), t);
                        }

                        if (metadata.tokenRequestUri == null) {
                            throw new ConfigurationException(
                                    "OpenId Connect Provider token endpoint URL is not configured and can not be discovered");
                        }
                        String grantType = oidcConfig.grant.getType().getGrantType();

                        MultiMap tokenGrantParams = null;

                        if (oidcConfig.grant.getType() != Grant.Type.REFRESH) {
                            tokenGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
                            setGrantClientParams(oidcConfig, tokenGrantParams, grantType);

                            if (oidcConfig.getGrantOptions() != null) {
                                Map<String, String> grantOptions = oidcConfig.getGrantOptions()
                                        .get(oidcConfig.grant.getType().name().toLowerCase());
                                if (grantOptions != null) {
                                    if (oidcConfig.grant.getType() == Grant.Type.PASSWORD) {
                                        // Without this block `password` will be listed first, before `username`
                                        // which is not a technical problem but might affect Wiremock tests or the endpoints
                                        // which expect a specific order.
                                        final String userName = grantOptions.get(OidcConstants.PASSWORD_GRANT_USERNAME);
                                        final String userPassword = grantOptions.get(OidcConstants.PASSWORD_GRANT_PASSWORD);
                                        if (userName == null || userPassword == null) {
                                            throw new ConfigurationException(
                                                    "Username and password must be set when a password grant is used",
                                                    Set.of("quarkus.oidc-client.grant.type",
                                                            "quarkus.oidc-client.grant-options"));
                                        }
                                        tokenGrantParams.add(OidcConstants.PASSWORD_GRANT_USERNAME, userName);
                                        tokenGrantParams.add(OidcConstants.PASSWORD_GRANT_PASSWORD, userPassword);
                                        for (Map.Entry<String, String> entry : grantOptions.entrySet()) {
                                            if (!OidcConstants.PASSWORD_GRANT_USERNAME.equals(entry.getKey())
                                                    && !OidcConstants.PASSWORD_GRANT_PASSWORD.equals(entry.getKey())) {
                                                tokenGrantParams.add(entry.getKey(), entry.getValue());
                                            }
                                        }
                                    } else {
                                        tokenGrantParams.addAll(grantOptions);
                                    }
                                }
                            }
                        }

                        MultiMap commonRefreshGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
                        setGrantClientParams(oidcConfig, commonRefreshGrantParams, OidcConstants.REFRESH_TOKEN_GRANT);

                        return new OidcClientImpl(client, metadata.tokenRequestUri, metadata.tokenRevokeUri, grantType,
                                tokenGrantParams,
                                commonRefreshGrantParams,
                                oidcConfig,
                                oidcRequestFilters);
                    }

                });
    }

    private static String getEndpointUrl(OidcClientConfig oidcConfig) {
        return oidcConfig.authServerUrl.isPresent() ? oidcConfig.authServerUrl.get() : oidcConfig.tokenPath.get();
    }

    private static void setGrantClientParams(OidcClientConfig oidcConfig, MultiMap grantParams, String grantType) {
        grantParams.add(OidcConstants.GRANT_TYPE, grantType);
        if (oidcConfig.getScopes().isPresent()) {
            grantParams.add(OidcConstants.TOKEN_SCOPE, oidcConfig.getScopes().get().stream().collect(Collectors.joining(" ")));
        }
    }

    private static Uni<OidcConfigurationMetadata> discoverTokenUris(WebClient client,
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> oidcRequestFilters,
            String authServerUrl, OidcClientConfig oidcConfig, io.vertx.mutiny.core.Vertx vertx) {
        final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
        return OidcCommonUtils
                .discoverMetadata(client, oidcRequestFilters, authServerUrl, connectionDelayInMillisecs, vertx,
                        oidcConfig.useBlockingDnsLookup)
                .onItem().transform(json -> new OidcConfigurationMetadata(json.getString("token_endpoint"),
                        json.getString("revocation_endpoint")));
    }

    protected static OidcClientException toOidcClientException(String authServerUrlString, Throwable cause) {
        return new OidcClientException(OidcCommonUtils.formatConnectionErrorMessage(authServerUrlString), cause);
    }

    private static class DisabledOidcClient implements OidcClient {
        String message;

        DisabledOidcClient(String message) {
            this.message = message;
        }

        @Override
        public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
            throw new DisabledOidcClientException(message);
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
            throw new DisabledOidcClientException(message);
        }

        @Override
        public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
            throw new DisabledOidcClientException(message);
        }

        @Override
        public void close() throws IOException {
        }

    }

    private static class OidcConfigurationMetadata {
        private final String tokenRequestUri;
        private final String tokenRevokeUri;

        OidcConfigurationMetadata(String tokenRequestUri, String tokenRevokeUri) {
            this.tokenRequestUri = tokenRequestUri;
            this.tokenRevokeUri = tokenRevokeUri;
        }
    }
}
