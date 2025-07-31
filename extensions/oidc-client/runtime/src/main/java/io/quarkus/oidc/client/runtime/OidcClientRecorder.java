package io.quarkus.oidc.client.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.enterprise.inject.CreationException;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.oidc.common.runtime.OidcTlsSupport;
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
    private static final String CLIENT_ID_ATTRIBUTE = "client-id";
    static final String DEFAULT_OIDC_CLIENT_ID = "Default";

    static Map<String, OidcClient> createStaticOidcClients(OidcClientsConfig oidcClientsConfig, Vertx vertx,
            OidcTlsSupport tlsSupport, OidcClientConfig defaultClientConfig) {

        String defaultClientId = defaultClientConfig.id().get();

        Map<String, OidcClient> staticOidcClients = new HashMap<>();

        for (var config : oidcClientsConfig.namedClients().entrySet()) {
            final String namedKey = config.getKey();
            if (!OidcClientsConfig.DEFAULT_CLIENT_KEY.equals(namedKey)) {
                var namedOidcClientConfig = config.getValue();
                OidcCommonUtils.verifyConfigurationId(defaultClientId, namedKey, namedOidcClientConfig.id());
                staticOidcClients.put(namedKey, createOidcClient(namedOidcClientConfig, namedKey, vertx, tlsSupport));
            }
        }

        return Map.copyOf(staticOidcClients);
    }

    public Function<SyntheticCreationalContext<OidcClient>, OidcClient> createOidcClientBean(String clientName) {
        return new Function<SyntheticCreationalContext<OidcClient>, OidcClient>() {
            @Override
            public OidcClient apply(SyntheticCreationalContext<OidcClient> ctx) {
                return ctx.getInjectedReference(OidcClients.class).getClient(clientName);
            }
        };
    }

    protected static OidcClient createOidcClient(OidcClientConfig oidcConfig, String oidcClientId, Vertx vertx,
            OidcTlsSupport tlsSupport) {
        return createOidcClientUni(oidcConfig, oidcClientId, vertx, tlsSupport).await()
                .atMost(oidcConfig.connectionTimeout());
    }

    protected static Uni<OidcClient> createOidcClientUni(OidcClientConfig oidcConfig, String oidcClientId,
            Vertx vertx, OidcTlsSupport tlsSupport) {
        if (!oidcConfig.clientEnabled()) {
            String message = String.format("'%s' client configuration is disabled", oidcClientId);
            LOG.debug(message);
            return Uni.createFrom().item(new DisabledOidcClient(message));
        }
        if (oidcConfig.id().isEmpty()) {
            // if user did not set the client id
            // we do set 'id' to the named client key
            // e.g. quarkus.oidc-client.<<name>>.id=<<name>>
            return Uni.createFrom().failure(new IllegalStateException("OIDC Client ID must be set"));
        }

        try {
            if (oidcConfig.authServerUrl().isEmpty() && !OidcCommonUtils.isAbsoluteUrl(oidcConfig.tokenPath())) {
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
        options.setFollowRedirects(oidcConfig.followRedirects());
        OidcCommonUtils.setHttpClientOptions(oidcConfig, options, tlsSupport.forConfig(oidcConfig.tls()));

        var mutinyVertx = new io.vertx.mutiny.core.Vertx(vertx);
        WebClient client = WebClient.create(mutinyVertx, options);

        OidcFilterStorage oidcFilterStorage = OidcFilterStorage.get();
        Uni<OidcConfigurationMetadata> tokenUrisUni = null;
        if (OidcCommonUtils.isAbsoluteUrl(oidcConfig.tokenPath())) {
            tokenUrisUni = Uni.createFrom().item(
                    new OidcConfigurationMetadata(oidcConfig.tokenPath().get(),
                            OidcCommonUtils.isAbsoluteUrl(oidcConfig.revokePath()) ? oidcConfig.revokePath().get() : null));
        } else {
            String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);
            if (!oidcConfig.discoveryEnabled().orElse(true)) {
                tokenUrisUni = Uni.createFrom()
                        .item(new OidcConfigurationMetadata(
                                OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.tokenPath()),
                                OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.revokePath())));
            } else {
                tokenUrisUni = discoverTokenUris(client, oidcFilterStorage, authServerUriString, oidcConfig, mutinyVertx);
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
                        String grantType = oidcConfig.grant().type().getGrantType();

                        MultiMap tokenGrantParams = null;

                        if (oidcConfig.grant().type() != Grant.Type.REFRESH) {
                            tokenGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
                            setGrantClientParams(oidcConfig, tokenGrantParams, grantType);

                            if (oidcConfig.grantOptions() != null) {
                                Map<String, String> grantOptions = oidcConfig.grantOptions()
                                        .get(oidcConfig.grant().type().name().toLowerCase());
                                if (grantOptions != null) {
                                    if (oidcConfig.grant().type() == Grant.Type.PASSWORD) {
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
                                if (oidcConfig.grant().type() == Grant.Type.EXCHANGE
                                        && !tokenGrantParams.contains(OidcConstants.EXCHANGE_GRANT_SUBJECT_TOKEN_TYPE)) {
                                    tokenGrantParams.add(OidcConstants.EXCHANGE_GRANT_SUBJECT_TOKEN_TYPE,
                                            OidcConstants.EXCHANGE_GRANT_SUBJECT_ACCESS_TOKEN_TYPE);
                                }
                            }
                        }

                        MultiMap commonRefreshGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
                        setGrantClientParams(oidcConfig, commonRefreshGrantParams, OidcConstants.REFRESH_TOKEN_GRANT);

                        return new OidcClientImpl(client, metadata.tokenRequestUri, metadata.tokenRevokeUri, grantType,
                                tokenGrantParams, commonRefreshGrantParams, oidcConfig, vertx, oidcFilterStorage);
                    }

                });
    }

    private static String getEndpointUrl(OidcClientConfig oidcConfig) {
        return oidcConfig.authServerUrl().isPresent() ? oidcConfig.authServerUrl().get() : oidcConfig.tokenPath().get();
    }

    private static void setGrantClientParams(OidcClientConfig oidcConfig, MultiMap grantParams, String grantType) {
        grantParams.add(OidcConstants.GRANT_TYPE, grantType);
        if (oidcConfig.scopes().isPresent()) {
            grantParams.add(OidcConstants.TOKEN_SCOPE, String.join(" ", oidcConfig.scopes().get()));
        }
        if (oidcConfig.audience().isPresent()) {
            grantParams.add(OidcConstants.TOKEN_AUDIENCE_GRANT_PROPERTY, String.join(" ", oidcConfig.audience().get()));
        }
    }

    private static Uni<OidcConfigurationMetadata> discoverTokenUris(WebClient client, OidcFilterStorage oidcFilterStorage,
            String authServerUrl, OidcClientConfig oidcConfig, io.vertx.mutiny.core.Vertx vertx) {
        final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
        OidcRequestContextProperties contextProps = new OidcRequestContextProperties(
                Map.of(CLIENT_ID_ATTRIBUTE, oidcConfig.id().orElse(DEFAULT_OIDC_CLIENT_ID)));
        return OidcCommonUtils
                .discoverMetadata(client, contextProps, authServerUrl, connectionDelayInMillisecs, vertx,
                        oidcConfig.useBlockingDnsLookup(), oidcFilterStorage)
                .onItem().transform(json -> new OidcConfigurationMetadata(json.getString("token_endpoint"),
                        json.getString("revocation_endpoint")));
    }

    protected static OidcClientException toOidcClientException(String authServerUrlString, Throwable cause) {
        return new OidcClientException(OidcCommonUtils.formatConnectionErrorMessage(authServerUrlString), cause);
    }

    public void initOidcClients() {
        try {
            // makes sure that OIDC Clients are created at the latest when runtime synthetic beans are ready
            Arc.container().instance(OidcClients.class).get();
        } catch (CreationException wrapper) {
            if (wrapper.getCause() instanceof RuntimeException runtimeException) {
                // so that users see ConfigurationException etc. without noise
                throw runtimeException;
            }
            throw wrapper;
        }
    }

    private static class DisabledOidcClient implements OidcClient {
        String message;

        DisabledOidcClient(String message) {
            this.message = message;
        }

        @Override
        public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
            return Uni.createFrom().failure(new DisabledOidcClientException(message));
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
            return Uni.createFrom().failure(new DisabledOidcClientException(message));
        }

        @Override
        public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
            return Uni.createFrom().failure(new DisabledOidcClientException(message));
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
