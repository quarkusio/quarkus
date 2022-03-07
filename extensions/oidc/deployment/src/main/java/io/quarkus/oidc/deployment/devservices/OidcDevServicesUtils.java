package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.Map;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
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
    private OidcDevServicesUtils() {

    }

    public static WebClient createWebClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true);
        options.setVerifyHost(false);
        return WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);
    }

    public static String getPasswordAccessToken(WebClient client,
            String tokenUrl,
            String clientId,
            String clientSecret,
            String userName,
            String userPassword,
            Map<String, String> passwordGrantOptions,
            Duration timeout) throws Exception {
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
                .transform(resp -> getAccessTokenFromJson(resp)).await().atMost(timeout);
    }

    public static String getClientCredAccessToken(WebClient client,
            String tokenUrl,
            String clientId,
            String clientSecret,
            Map<String, String> clientCredGrantOptions,
            Duration timeout) throws Exception {
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
