package io.quarkus.test.oidc.client;

import static org.awaitility.Awaitility.await;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;

public class OidcTestClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private final static String CLIENT_AUTH_SERVER_URL_PROP = "client.quarkus.oidc.auth-server-url";
    private final static String AUTH_SERVER_URL_PROP = "quarkus.oidc.auth-server-url";
    private final static String CLIENT_ID_PROP = "quarkus.oidc.client-id";
    private final static String CLIENT_SECRET_PROP = "quarkus.oidc.credentials.secret";

    private volatile Vertx vertx = null;
    private volatile WebClient client = null;

    private String authServerUrl;
    private String tokenUrl;

    /**
     * Get an access token a client_credentials grant.
     * Client id must be configured with the `quarkus.oidc.client-id` property.
     * Client secret must be configured with the `quarkus.oidc.credentials.secret` property.
     */
    public String getClientAccessToken() {
        return getClientAccessToken(null);
    }

    /**
     * Get an access token a client_credentials grant with additional properties.
     * Client id must be configured with the `quarkus.oidc.client-id` property.
     * Client secret must be configured with the `quarkus.oidc.credentials.secret` property.
     */
    public String getClientAccessToken(Map<String, String> extraProps) {
        return getClientAccessToken(getClientId(), getClientSecret(), extraProps);
    }

    /**
     * Get an access token from the default tenant realm using a client_credentials grant with a
     * the provided client id and secret.
     */
    public String getClientAccessToken(String clientId, String clientSecret) {
        return getClientAccessToken(clientId, clientSecret, null);
    }

    /**
     * Get an access token using a client_credentials grant with the provided client id and secret,
     * and additional properties.
     */
    public String getClientAccessToken(String clientId, String clientSecret, Map<String, String> extraProps) {
        MultiMap requestMap = MultiMap.caseInsensitiveMultiMap();
        requestMap.add("grant_type", "client_credentials")
                .add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            requestMap.add("client_secret", clientSecret);
        }
        return getAccessTokenInternal(requestMap, extraProps);
    }

    /**
     * Get an access token from the default tenant realm using a password grant with the provided user name, user secret.
     * Client id must be configured with the `quarkus.oidc.client-id` property.
     * Client secret must be configured with the `quarkus.oidc.credentials.secret` property.
     */
    public String getAccessToken(String userName, String userSecret) {
        return getAccessToken(userName, userSecret, null);
    }

    /**
     * Get an access token from the default tenant realm using a password grant with the provided user name, user secret,
     * and additional properties.
     * Client id must be configured with the `quarkus.oidc.client-id` property.
     * Client secret must be configured with the `quarkus.oidc.credentials.secret` property.
     */
    public String getAccessToken(String userName, String userSecret, Map<String, String> extraProps) {
        return getAccessToken(getClientId(), getClientSecret(), userName, userSecret, extraProps);
    }

    /**
     * Get an access token from the default tenant realm using a password grant with the provided client id, client secret, user
     * name, user secret, client
     * id and user secret.
     */
    public String getAccessToken(String clientId, String clientSecret, String userName, String userSecret) {
        return getAccessToken(userName, userSecret, clientId, clientSecret, null);
    }

    /**
     * Get an access token using a password grant with the provided user name, user secret, client
     * id and secret, and scopes.
     */
    public String getAccessToken(String clientId, String clientSecret, String userName, String userSecret,
            Map<String, String> extraProps) {

        MultiMap requestMap = MultiMap.caseInsensitiveMultiMap();
        requestMap.add("grant_type", "password")
                .add("username", userName)
                .add("password", userSecret);

        requestMap.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            requestMap.add("client_secret", clientSecret);
        }
        return getAccessTokenInternal(requestMap, extraProps);
    }

    private String getAccessTokenInternal(MultiMap requestMap, Map<String, String> extraProps) {

        if (extraProps != null) {
            requestMap = requestMap.addAll(extraProps);
        }

        var result = getClient().postAbs(getTokenUrl())
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendBuffer(encodeForm(requestMap));
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);

        return result.result().bodyAsJsonObject().getString("access_token");
    }

    private String getClientId() {
        return getPropertyValue(CLIENT_ID_PROP);
    }

    private String getClientSecret() {
        return getPropertyValue(CLIENT_SECRET_PROP);
    }

    /**
     * Return URL string configured with a 'quarkus.oidc.auth-server' property.
     */
    public String getAuthServerUrl() {
        if (authServerUrl == null) {
            authServerUrl = getOptionalPropertyValue(CLIENT_AUTH_SERVER_URL_PROP, AUTH_SERVER_URL_PROP);
        }
        return authServerUrl;
    }

    /**
     * Return URL string configured with a 'quarkus.oidc.auth-server' property.
     */
    public String getTokenUrl() {
        if (tokenUrl == null) {
            getAuthServerUrl();
            var result = getClient().getAbs(authServerUrl + "/.well-known/openid-configuration")
                    .send();
            await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
            tokenUrl = result.result().bodyAsJsonObject().getString("token_endpoint");
        }
        return tokenUrl;
    }

    private String getPropertyValue(String prop) {
        return ConfigProvider.getConfig().getValue(prop, String.class);
    }

    private String getOptionalPropertyValue(String prop, String defaultProp) {
        return ConfigProvider.getConfig().getOptionalValue(prop, String.class)
                .orElseGet(() -> ConfigProvider.getConfig().getValue(defaultProp, String.class));
    }

    public static Buffer encodeForm(MultiMap form) {
        Buffer buffer = Buffer.buffer();
        for (Map.Entry<String, String> entry : form) {
            if (buffer.length() != 0) {
                buffer.appendByte((byte) '&');
            }
            buffer.appendString(entry.getKey());
            buffer.appendByte((byte) '=');
            buffer.appendString(urlEncode(entry.getValue()));
        }
        return buffer;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private WebClient getClient() {
        if (client == null) {
            client = WebClient.create(getVertx());
        }
        return client;
    }

    private Vertx getVertx() {
        final ArcContainer container = Arc.container();
        if (container != null && container.isRunning()) {
            var managedVertx = container.instance(Vertx.class).orElse(null);
            if (managedVertx != null) {
                return managedVertx;
            }
        }
        if (vertx == null) {
            vertx = Vertx.vertx();
        }
        return vertx;
    }

    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
            vertx = null;
        }
    }
}
