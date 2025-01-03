package io.quarkus.oidc.runtime.dev.ui;

import java.time.Duration;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public final class OidcDevServicesUtils {

    private static final Logger LOG = Logger.getLogger(OidcDevServicesUtils.class);
    private static final String APPLICATION_JSON = "application/json";

    private OidcDevServicesUtils() {

    }

    public static WebClient createWebClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true);
        options.setVerifyHost(false);
        return WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);
    }

    public static Uni<String> getPasswordAccessToken(WebClient client,
            String tokenUrl,
            String clientId,
            String clientSecret,
            String userName,
            String userPassword,
            Map<String, String> passwordGrantOptions) {
        HttpRequest<Buffer> request = client.postAbs(tokenUrl);
        request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());

        io.vertx.mutiny.core.MultiMap props = new io.vertx.mutiny.core.MultiMap(MultiMap.caseInsensitiveMultiMap());
        props.add("client_id", clientId);
        if (clientSecret != null) {
            props.add("client_secret", clientSecret);
        }

        props.add("username", userName);
        props.add("password", userPassword);
        props.add("grant_type", "password");
        if (passwordGrantOptions != null) {
            props.addAll(passwordGrantOptions);
        }

        return request.sendBuffer(OidcCommonUtils.encodeForm(props)).onItem()
                .transform(resp -> getAccessTokenFromJson(resp))
                .onFailure()
                .retry()
                .withBackOff(Duration.ofSeconds(2), Duration.ofSeconds(2))
                .expireIn(10 * 1000);
    }

    public static Uni<String> getClientCredAccessToken(WebClient client,
            String tokenUrl,
            String clientId,
            String clientSecret,
            Map<String, String> clientCredGrantOptions) {
        HttpRequest<Buffer> request = client.postAbs(tokenUrl);
        request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());

        io.vertx.mutiny.core.MultiMap props = new io.vertx.mutiny.core.MultiMap(MultiMap.caseInsensitiveMultiMap());
        props.add("client_id", clientId);
        if (clientSecret != null) {
            props.add("client_secret", clientSecret);
        }

        props.add("grant_type", "client_credentials");
        if (clientCredGrantOptions != null) {
            props.addAll(clientCredGrantOptions);
        }

        return request.sendBuffer(OidcCommonUtils.encodeForm(props)).onItem()
                .transform(resp -> getAccessTokenFromJson(resp));
    }

    public static Uni<String> getTokens(String tokenUrl, String clientId, String clientSecret,
            String authorizationCode, String redirectUri,
            Vertx vertxInstance, Map<String, String> grantOptions) {
        WebClient client = createWebClient(vertxInstance);

        LOG.infof("Using authorization_code grant to get a token from '%s' with client id '%s'",
                tokenUrl, clientId);

        HttpRequest<Buffer> request = client.postAbs(tokenUrl);
        request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());
        request.putHeader(HttpHeaders.ACCEPT.toString(), APPLICATION_JSON);

        io.vertx.mutiny.core.MultiMap props = new io.vertx.mutiny.core.MultiMap(MultiMap.caseInsensitiveMultiMap());
        props.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            props.add("client_secret", clientSecret);
        }
        props.add("grant_type", "authorization_code");
        props.add("code", authorizationCode);
        props.add("redirect_uri", redirectUri);
        if (grantOptions != null) {
            props.addAll(grantOptions);
        }

        return request
                .sendBuffer(OidcCommonUtils.encodeForm(props))
                .map(OidcDevServicesUtils::getBodyAsString)
                .onFailure().invoke(t -> LOG.errorf("Token can not be acquired from OpenId Connect provider: %s", t.toString()))
                .eventually(client::close);
    }

    public static Uni<Integer> testServiceWithToken(String serviceUrl, String token, Vertx vertxInstance) {
        LOG.infof("Test token: %s", token);
        LOG.infof("Sending token to '%s'", serviceUrl);
        WebClient client = createWebClient(vertxInstance);
        return client.getAbs(serviceUrl)
                .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                .send()
                .map(HttpResponse::statusCode)
                .invoke(statusCode -> LOG.infof("Result: %d", statusCode))
                .onFailure().invoke(t -> LOG.errorf("Token can not be sent to the service: %s", t.toString()))
                .eventually(client::close);
    }

    public static Uni<String> testServiceWithClientCred(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret, Vertx vertxInstance, Duration timeout,
            Map<String, String> clientCredGrantOptions) {
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        LOG.infof("Using a client_credentials grant to get a token token from '%s' with client id '%s'",
                tokenUrl, clientId);

        Uni<String> token = OidcDevServicesUtils.getClientCredAccessToken(client, tokenUrl, clientId, clientSecret,
                clientCredGrantOptions)
                .ifNoItem().after(timeout).fail()
                .invoke(t -> LOG.infof("Test token: %s", t))
                .onFailure()
                .invoke(t -> LOG.errorf("Token can not be acquired from OpenId Connect provider: %s", t.toString()));

        // no service url -> only token is required
        if (serviceUrl != null) {
            token = testServiceInternal(client, serviceUrl, token);
        }

        return token.eventually(client::close);
    }

    public static Uni<String> testServiceWithPassword(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret, String username, String password,
            Vertx vertxInstance, Duration timeout,
            Map<String, String> passwordGrantOptions,
            Map<String, String> usernameToPassword) {
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        LOG.infof("Using a password grant to get a token from '%s' for user '%s' with client id '%s'",
                tokenUrl, username, clientId);

        // user-defined password has preference over known passwords
        if (password == null || password.isBlank()) {
            password = usernameToPassword.get("password");
            if (password == null) {
                return Uni.createFrom().failure(
                        new IllegalArgumentException("Can't request access token as password is missing"));
            }
        }
        Uni<String> token = OidcDevServicesUtils.getPasswordAccessToken(client, tokenUrl,
                clientId, clientSecret, username, password, passwordGrantOptions)
                .ifNoItem().after(timeout).fail()
                .invoke(t -> LOG.infof("Test token: %s", t))
                .onFailure()
                .invoke(t -> LOG.errorf("Token can not be acquired from OpenId Connect provider: %s", t.toString()));

        // no service url -> only token is required
        if (serviceUrl != null) {
            token = testServiceInternal(client, serviceUrl, token);
        }

        return token.eventually(client::close);
    }

    private static Uni<String> testServiceInternal(WebClient client, String serviceUrl, Uni<String> tokenUni) {
        return tokenUni
                .flatMap(token -> {
                    LOG.infof("Sending token to '%s'", serviceUrl);
                    return client
                            .getAbs(serviceUrl)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                            .send()
                            .map(HttpResponse::statusCode)
                            .map(Object::toString)
                            .invoke(statusCode -> LOG.infof("Result: %s", statusCode))
                            .onFailure().invoke(t2 -> LOG.errorf("Token can not be sent to the service: %s",
                                    t2.toString()));
                });
    }

    private static String getBodyAsString(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            return resp.bodyAsString();
        } else {
            String errorMessage = resp.bodyAsString();
            throw new RuntimeException(errorMessage);
        }
    }

    private static String getAccessTokenFromJson(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            JsonObject json = resp.bodyAsJsonObject();
            return json.getString("access_token");
        } else {
            String errorMessage = resp.bodyAsString();
            throw new RuntimeException(errorMessage);
        }
    }
}
