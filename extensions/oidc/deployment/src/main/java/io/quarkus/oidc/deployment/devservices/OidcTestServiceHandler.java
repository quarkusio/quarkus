package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcTestServiceHandler extends DevConsolePostHandler {
    private static final Logger LOG = Logger.getLogger(OidcTestServiceHandler.class);

    Vertx vertxInstance;
    Duration timeout;

    public OidcTestServiceHandler(Vertx vertxInstance, Duration timeout) {
        this.vertxInstance = vertxInstance;
        this.timeout = timeout;
    }

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);

        try {
            testServiceInternal(event, client, form.get("serviceUrl"), form.get("token"));
        } finally {
            client.close();
        }
    }

    private void testServiceInternal(RoutingContext event, WebClient client, String serviceUrl, String token) {
        try {
            LOG.infof("Test token: %s", token);
            LOG.infof("Sending token to '%s'", serviceUrl);
            int statusCode = client.getAbs(serviceUrl)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token).send().await()
                    .atMost(timeout)
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
