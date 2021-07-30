package io.quarkus.oidc.deployment.devservices.keycloak;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class KeycloakAuthorizationCodePostHandler extends DevConsolePostHandler {
    private static final Logger LOG = Logger.getLogger(KeycloakAuthorizationCodePostHandler.class);

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        WebClient client = KeycloakDevServicesUtils.createWebClient();
        String keycloakUrl = form.get("keycloakUrl") + "/realms/" + form.get("realm") + "/protocol/openid-connect/token";

        try {
            LOG.infof("Using authorization_code grant to get a token from '%s' in realm '%s' with client id '%s'",
                    keycloakUrl, form.get("realm"), form.get("client"));

            HttpRequest<Buffer> request = client.postAbs(keycloakUrl);
            request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());

            io.vertx.mutiny.core.MultiMap props = new io.vertx.mutiny.core.MultiMap(MultiMap.caseInsensitiveMultiMap());
            props.add("client_id", form.get("client"));
            if (form.get("clientSecret") != null) {
                props.add("client_secret", form.get("clientSecret"));
            }
            props.add("grant_type", "authorization_code");
            props.add("code", form.get("authorizationCode"));
            props.add("redirect_uri", form.get("redirectUri"));

            String tokens = request.sendBuffer(OidcCommonUtils.encodeForm(props)).onItem()
                    .transform(resp -> getBodyAsString(resp))
                    .await().atMost(KeycloakDevServicesProcessor.capturedDevServicesConfiguration.webClienTimeout);

            event.put("tokens", tokens);

        } catch (Throwable t) {
            LOG.errorf("Token can not be acquired from Keycloak: %s", t.toString());
        } finally {
            client.close();
        }
    }

    @Override
    protected void actionSuccess(RoutingContext event) {
        event.response().setStatusCode(200);
        String tokens = (String) event.get("tokens");
        if (tokens != null) {
            event.response().end(tokens);
        }
    }

    private static String getBodyAsString(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            return resp.bodyAsString();
        } else {
            String errorMessage = resp.bodyAsString();
            throw new RuntimeException(errorMessage);
        }
    }
}
