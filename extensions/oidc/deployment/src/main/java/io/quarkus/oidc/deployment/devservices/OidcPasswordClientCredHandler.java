package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.oidc.runtime.devui.OidcDevServicesUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class OidcPasswordClientCredHandler extends DevConsolePostHandler {
    private static final Logger LOG = Logger.getLogger(OidcPasswordClientCredHandler.class);
    Map<String, String> users;

    Vertx vertxInstance;
    Duration timeout;
    Map<String, String> passwordGrantOptions;
    Map<String, String> clientCredGrantOptions;

    public OidcPasswordClientCredHandler(Vertx vertxInstance, Duration timeout, Map<String, Map<String, String>> grantOptions) {
        this(vertxInstance, timeout, Map.of(), grantOptions);
    }

    public OidcPasswordClientCredHandler(Vertx vertxInstance, Duration timeout, Map<String, String> users,
            Map<String, Map<String, String>> grantOptions) {
        this.vertxInstance = vertxInstance;
        this.timeout = timeout;
        this.users = users;
        this.passwordGrantOptions = grantOptions.get("password");
        this.clientCredGrantOptions = grantOptions.get("client");
    }

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        final Uni<String> result;
        if ("password".equals(form.get("grant"))) {
            result = OidcDevServicesUtils
                    .testServiceWithPassword(form.get("tokenUrl"), form.get("serviceUrl"), form.get("client"),
                            form.get("clientSecret"), form.get("user"), form.get("password"), vertxInstance, timeout,
                            passwordGrantOptions, users);
        } else {
            result = OidcDevServicesUtils
                    .testServiceWithClientCred(form.get("tokenUrl"), form.get("serviceUrl"), form.get("client"),
                            form.get("clientSecret"), vertxInstance, timeout, clientCredGrantOptions);
        }
        event.put("result", result
                .onFailure().recoverWithNull()
                .await().indefinitely());
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
