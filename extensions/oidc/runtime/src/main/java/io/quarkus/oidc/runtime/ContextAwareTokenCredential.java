package io.quarkus.oidc.runtime;

import io.quarkus.security.credential.TokenCredential;
import io.vertx.ext.web.RoutingContext;

public class ContextAwareTokenCredential extends TokenCredential {

    private RoutingContext context;

    protected ContextAwareTokenCredential(String token, String type, RoutingContext context) {
        super(token, type);
        this.context = context;
    }

    RoutingContext getContext() {
        return context;
    }
}
