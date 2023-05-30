package io.quarkus.oidc.deployment.devservices;

import static io.quarkus.oidc.runtime.devui.OidcDevServicesUtils.testServiceWithToken;

import java.time.Duration;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

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
        testServiceInternal(event, form.get("serviceUrl"), form.get("token"));
    }

    private void testServiceInternal(RoutingContext event, String serviceUrl, String token) {
        var statusCode = testServiceWithToken(serviceUrl, token, vertxInstance)
                .onFailure().recoverWithNull()
                .await()
                .atMost(timeout);
        event.put("result", String.valueOf(statusCode));
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
