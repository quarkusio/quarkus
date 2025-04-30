package io.quarkus.devservices.keycloak;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

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

final class KeycloakDevServicesUtils {

    private static final byte AMP = '&';
    private static final byte EQ = '=';

    private KeycloakDevServicesUtils() {

    }

    static WebClient createWebClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true);
        options.setVerifyHost(false);
        return WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);
    }

    static Uni<String> getPasswordAccessToken(WebClient client,
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

        return request.sendBuffer(encodeForm(props)).onItem()
                .transform(KeycloakDevServicesUtils::getAccessTokenFromJson)
                .onFailure()
                .retry()
                .withBackOff(Duration.ofSeconds(2), Duration.ofSeconds(2))
                .expireIn(10 * 1000);
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

    private static Buffer encodeForm(io.vertx.mutiny.core.MultiMap form) {
        Buffer buffer = Buffer.buffer();
        for (Map.Entry<String, String> entry : form) {
            if (buffer.length() != 0) {
                buffer.appendByte(AMP);
            }
            buffer.appendString(entry.getKey());
            buffer.appendByte(EQ);
            buffer.appendString(urlEncode(entry.getValue()));
        }
        return buffer;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
