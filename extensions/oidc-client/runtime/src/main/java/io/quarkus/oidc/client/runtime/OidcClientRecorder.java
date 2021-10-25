package io.quarkus.oidc.client.runtime;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials;
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
    private static final Duration CONNECTION_BACKOFF_DURATION = Duration.ofSeconds(2);

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
        return createOidcClientUni(oidcConfig, oidcClientId, tlsConfig, vertx).await().indefinitely();
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
            OidcCommonUtils.verifyCommonConfiguration(oidcConfig, false, false);
        } catch (Throwable t) {
            LOG.debug(t.getMessage());
            String message = String.format("'%s' client configuration is not initialized", oidcClientId);
            return Uni.createFrom().item(new DisabledOidcClient(message));
        }

        String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);
        WebClientOptions options = new WebClientOptions();

        OidcCommonUtils.setHttpClientOptions(oidcConfig, tlsConfig, options);

        WebClient client = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx.get()), options);

        Uni<String> tokenRequestUriUni = null;
        if (!oidcConfig.discoveryEnabled) {
            tokenRequestUriUni = Uni.createFrom()
                    .item(OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.tokenPath));
        } else {
            tokenRequestUriUni = discoverTokenRequestUri(client, authServerUriString.toString(), oidcConfig);
        }
        return tokenRequestUriUni.onItemOrFailure()
                .transform(new BiFunction<String, Throwable, OidcClient>() {

                    @Override
                    public OidcClient apply(String tokenRequestUri, Throwable t) {
                        if (t != null) {
                            throw toOidcClientException(authServerUriString, t);
                        }

                        if (tokenRequestUri == null) {
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
                                        tokenGrantParams.add(OidcConstants.PASSWORD_GRANT_USERNAME,
                                                grantOptions.get(OidcConstants.PASSWORD_GRANT_USERNAME));
                                        tokenGrantParams.add(OidcConstants.PASSWORD_GRANT_PASSWORD,
                                                grantOptions.get(OidcConstants.PASSWORD_GRANT_PASSWORD));
                                    } else {
                                        tokenGrantParams.addAll(grantOptions);
                                    }
                                }
                            }
                        }

                        MultiMap commonRefreshGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
                        setGrantClientParams(oidcConfig, commonRefreshGrantParams, OidcConstants.REFRESH_TOKEN_GRANT);

                        return new OidcClientImpl(client, tokenRequestUri, grantType, tokenGrantParams,
                                commonRefreshGrantParams,
                                oidcConfig);
                    }
                });
    }

    private static void setGrantClientParams(OidcClientConfig oidcConfig, MultiMap grantParams, String grantType) {
        grantParams.add(OidcConstants.GRANT_TYPE, grantType);
        Credentials creds = oidcConfig.getCredentials();
        if (OidcCommonUtils.isClientSecretPostAuthRequired(creds)) {
            grantParams.add(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
            grantParams.add(OidcConstants.CLIENT_SECRET, OidcCommonUtils.clientSecret(creds));
        }
        if (oidcConfig.getScopes().isPresent()) {
            grantParams.add(OidcConstants.TOKEN_SCOPE, oidcConfig.getScopes().get().stream().collect(Collectors.joining(" ")));
        }
    }

    private static Uni<String> discoverTokenRequestUri(WebClient client, String authServerUrl, OidcClientConfig oidcConfig) {
        final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
        return OidcCommonUtils.discoverMetadata(client, authServerUrl, connectionDelayInMillisecs)
                .onItem().transform(json -> json.getString("token_endpoint"));
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
        public Uni<Tokens> getTokens(Map<String, String> grantParameters) {
            throw new DisabledOidcClientException(message);
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken) {
            throw new DisabledOidcClientException(message);
        }

        @Override
        public void close() throws IOException {
        }
    }
}
