package io.quarkus.oidc.deployment.devservices;

import static io.quarkus.oidc.runtime.devui.OidcDevServicesUtils.getTokens;

import java.time.Duration;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class OidcAuthorizationCodePostHandler extends DevConsolePostHandler {

    private static final Logger LOG = Logger.getLogger(OidcAuthorizationCodePostHandler.class);
    Vertx vertxInstance;
    Duration timeout;
    Map<String, String> grantOptions;

    public OidcAuthorizationCodePostHandler(Vertx vertxInstance, Duration timeout,
            Map<String, Map<String, String>> grantOptions) {
        this.vertxInstance = vertxInstance;
        this.timeout = timeout;
        this.grantOptions = grantOptions.get("code");
    }

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        try {
            final String tokens = getTokens(
                    form.get("tokenUrl"),
                    form.get("client"),
                    form.get("clientSecret"),
                    form.get("authorizationCode"),
                    form.get("redirectUri"),
                    vertxInstance,
                    grantOptions)
                    .onFailure().recoverWithNull()
                    .await().atMost(timeout);
            event.put("tokens", tokens);
        } catch (Throwable t) {
            LOG.errorf("Token can not be acquired from OpenId Connect provider: %s", t.toString());
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

}
