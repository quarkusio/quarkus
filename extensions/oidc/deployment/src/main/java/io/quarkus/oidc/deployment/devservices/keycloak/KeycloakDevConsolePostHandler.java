package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.oidc.deployment.devservices.OidcDevServicesUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;

public class KeycloakDevConsolePostHandler extends DevConsolePostHandler {
    private static final Logger LOG = Logger.getLogger(KeycloakDevConsolePostHandler.class);
    Map<String, String> users;

    public KeycloakDevConsolePostHandler(Map<String, String> users) {
        this.users = users;
    }

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        WebClient client = OidcDevServicesUtils.createWebClient(KeycloakDevServicesProcessor.vertxInstance);
        String tokenUrl = form.get("tokenUrl");

        try {
            String token = null;
            if ("password".equals(form.get("grant"))) {
                LOG.infof("Using a password grant to get a token from '%s' for user '%s' with client id '%s'",
                        tokenUrl, form.get("user"), form.get("client"));

                String userName = form.get("user");
                token = OidcDevServicesUtils.getPasswordAccessToken(client, tokenUrl,
                        form.get("client"), form.get("clientSecret"),
                        userName,
                        users.get(userName),
                        KeycloakDevServicesProcessor.capturedDevServicesConfiguration.webClienTimeout);
            } else {
                LOG.infof("Using a client_credentials grant to get a token token from '%s' with client id '%s'",
                        tokenUrl, form.get("client"));

                token = OidcDevServicesUtils.getClientCredAccessToken(client, tokenUrl,
                        form.get("client"),
                        form.get("clientSecret"),
                        KeycloakDevServicesProcessor.capturedDevServicesConfiguration.webClienTimeout);
            }

            testServiceInternal(event, client, form.get("serviceUrl"), token);
        } catch (Throwable t) {
            LOG.errorf("Token can not be acquired from Keycloak: %s", t.toString());
        } finally {
            client.close();
        }
    }

    private void testServiceInternal(RoutingContext event, WebClient client, String serviceUrl, String token) {
        try {
            LOG.infof("Test token: %s", token);
            LOG.infof("Sending token to '%s'", serviceUrl);
            int statusCode = client.getAbs(serviceUrl)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token).send().await().indefinitely()
                    .statusCode();
            LOG.infof("Result: %d", statusCode);
            event.put("result", String.valueOf(statusCode));
        } catch (Throwable t) {
            LOG.errorf("Token can not be sent to the service: %s", t.toString());
        }
    }

    @Override
    protected void actionSuccess(RoutingContext event) {
        event.response().setStatusCode(200);
        String result = (String) event.get("result");
        if (result != null) {
            event.response().end(result);
        }
    }
}
