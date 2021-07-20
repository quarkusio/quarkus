package io.quarkus.oidc.deployment.devservices.keycloak;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;

public class KeycloakImplicitGrantPostHandler extends DevConsolePostHandler {
    private static final Logger LOG = Logger.getLogger(KeycloakImplicitGrantPostHandler.class);

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        WebClient client = KeycloakDevServicesUtils.createWebClient();

        try {
            String token = form.get("token");

            LOG.infof("Test token: %s", token);
            LOG.infof("Sending token to '%s'", form.get("serviceUrl"));
            testServiceInternal(event, client, form.get("serviceUrl"), token);
        } catch (Throwable t) {
            LOG.errorf("Token can not be acquired from Keycloak: %s", t.toString());
        } finally {
            client.close();
        }
    }

    private void testServiceInternal(RoutingContext event, WebClient client, String serviceUrl, String token) {
        try {
            int statusCode = client.getAbs(serviceUrl)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token).send().await()
                    .atMost(KeycloakDevServicesProcessor.capturedDevServicesConfiguration.webClienTimeout)
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
