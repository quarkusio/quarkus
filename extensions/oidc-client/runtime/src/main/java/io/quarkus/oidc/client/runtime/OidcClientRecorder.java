package io.quarkus.oidc.client.runtime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
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
import io.vertx.core.json.JsonObject;
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

        OidcCommonUtils.verifyCommonConfiguration(oidcConfig);

        String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);

        WebClientOptions options = new WebClientOptions();

        URI authServerUri = URI.create(authServerUriString);

        OidcCommonUtils.setHttpClientOptions(oidcConfig, tlsConfig, options);
        setWebClientOptions(authServerUri, options);

        WebClient client = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx.get()), options);

        Uni<String> tokenRequestUriUni = null;
        if (!oidcConfig.discoveryEnabled) {
            tokenRequestUriUni = Uni.createFrom()
                    .item(OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.tokenPath));
        } else {
            tokenRequestUriUni = discoverTokenRequestUri(client, authServerUriString, oidcConfig);
        }
        return tokenRequestUriUni.onItem().transform(new Function<String, OidcClient>() {

            @Override
            public OidcClient apply(String tokenRequestUri) {
                if (tokenRequestUri == null) {
                    throw new ConfigurationException(
                            "OpenId Connect Provider token endpoint URL is not configured and can not be discovered");
                }
                MultiMap tokenGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());

                String grantType = oidcConfig.grant.getType() == Grant.Type.CLIENT
                        ? OidcConstants.CLIENT_CREDENTIALS_GRANT
                        : OidcConstants.PASSWORD_GRANT;
                setGrantClientParams(oidcConfig, tokenGrantParams, grantType);

                if (oidcConfig.grant.getType() == Grant.Type.PASSWORD) {
                    Map<String, String> passwordGrantOptions = oidcConfig.getGrantOptions().get(OidcConstants.PASSWORD_GRANT);
                    tokenGrantParams.add(OidcConstants.PASSWORD_GRANT_USERNAME,
                            passwordGrantOptions.get(OidcConstants.PASSWORD_GRANT_USERNAME));
                    tokenGrantParams.add(OidcConstants.PASSWORD_GRANT_PASSWORD,
                            passwordGrantOptions.get(OidcConstants.PASSWORD_GRANT_PASSWORD));
                }

                MultiMap commonRefreshGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
                setGrantClientParams(oidcConfig, commonRefreshGrantParams, OidcConstants.REFRESH_TOKEN_GRANT);

                return new OidcClientImpl(client, tokenRequestUri, grantType, tokenGrantParams, commonRefreshGrantParams,
                        oidcConfig);
            }
        });
    }

    private static void setWebClientOptions(URI authServerUri, WebClientOptions options) {
        try {
            URL url = authServerUri.toURL();
            options.setDefaultHost(authServerUri.getHost());
            boolean ssl = false;
            int port = url.getPort();
            String protocol = url.getProtocol();
            char chend = protocol.charAt(protocol.length() - 1);
            if (chend == 'p') {
                if (port == -1) {
                    port = 80;
                }
            } else if (chend == 's') {
                ssl = true;
                if (port == -1) {
                    port = 443;
                }
            }
            options.setDefaultPort(port);
            options.setSsl(ssl);
        } catch (MalformedURLException e) {
            throw new OidcClientException("Please check oidc-client auth-server-url");
        }
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
        final long connectionRetryCount = OidcCommonUtils.getConnectionRetryCount(oidcConfig);
        if (connectionRetryCount > 1) {
            LOG.infof("Connecting to IDP for up to %d times every 2 seconds", connectionRetryCount);
        }

        for (long i = 0; i < connectionRetryCount; i++) {
            try {
                if (oidcConfig.discoveryEnabled) {
                    return discoverTokenEndpoint(client, authServerUrl);
                }
                break;
            } catch (Throwable throwable) {
                while (throwable instanceof CompletionException && throwable.getCause() != null) {
                    throwable = throwable.getCause();
                }
                if (throwable instanceof OidcClientException) {
                    if (i + 1 < connectionRetryCount) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException iex) {
                            // continue connecting
                        }
                    } else {
                        throw (OidcClientException) throwable;
                    }
                } else {
                    throw new OidcClientException(throwable);
                }
            }
        }
        return Uni.createFrom().nullItem();
    }

    private static Uni<String> discoverTokenEndpoint(WebClient client, String authServerUrl) {
        String discoveryUrl = authServerUrl + "/.well-known/openid-configuration";
        return client.get(discoveryUrl).send().onItem().transformToUni(resp -> {
            if (resp.statusCode() == 200) {
                JsonObject json = resp.bodyAsJsonObject();
                return Uni.createFrom().item(json.getString("token_endpoint"));
            } else {
                LOG.tracef("Discovery has failed, status code: %d", resp.statusCode());
                return Uni.createFrom().nullItem();
            }
        });
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
        public Uni<Tokens> getTokens() {
            throw new OidcClientException(message);
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken) {
            throw new OidcClientException(message);
        }

        @Override
        public void close() throws IOException {
            throw new OidcClientException(message);
        }
    }
}
