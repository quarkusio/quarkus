package io.quarkus.oidc.deployment.devservices.keycloak;

import java.time.Duration;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public final class KeycloakDevServicesUtils {
    private KeycloakDevServicesUtils() {

    }

    public static WebClient createWebClient() {
        return WebClient.create(new io.vertx.mutiny.core.Vertx(KeycloakDevServicesProcessor.vertxInstance));
    }

    public static String getPasswordAccessToken(WebClient client,
            String keycloakUrl,
            String clientId,
            String clientSecret,
            String userName,
            String userPassword,
            Duration timeout) throws Exception {
        HttpRequest<Buffer> request = client.postAbs(keycloakUrl);
        request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());

        io.vertx.mutiny.core.MultiMap props = new io.vertx.mutiny.core.MultiMap(MultiMap.caseInsensitiveMultiMap());
        props.add("client_id", clientId);
        if (clientSecret != null) {
            props.add("client_secret", clientSecret);
        }

        props.add("username", userName);
        props.add("password", userPassword);
        props.add("grant_type", "password");

        return request.sendBuffer(OidcCommonUtils.encodeForm(props)).onItem()
                .transform(resp -> getAccessTokenFromJson(resp)).await().atMost(timeout);
    }

    public static String getClientCredAccessToken(WebClient client,
            String keycloakUrl,
            String clientId,
            String clientSecret,
            Duration timeout) throws Exception {
        HttpRequest<Buffer> request = client.postAbs(keycloakUrl);
        request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());

        io.vertx.mutiny.core.MultiMap props = new io.vertx.mutiny.core.MultiMap(MultiMap.caseInsensitiveMultiMap());
        props.add("client_id", clientId);
        if (clientSecret != null) {
            props.add("client_secret", clientSecret);
        }

        props.add("grant_type", "client_credentials");

        return request.sendBuffer(OidcCommonUtils.encodeForm(props)).onItem()
                .transform(resp -> getAccessTokenFromJson(resp)).await().atMost(timeout);
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
