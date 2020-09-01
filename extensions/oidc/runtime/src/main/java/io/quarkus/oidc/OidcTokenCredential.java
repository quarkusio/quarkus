package io.quarkus.oidc;

import io.quarkus.security.credential.TokenCredential;
import io.vertx.ext.web.RoutingContext;

public class OidcTokenCredential extends TokenCredential {

    private RoutingContext context;

    protected OidcTokenCredential(String token, String type, RoutingContext context) {
        super(token, type);
        this.context = context;
    }

    public RoutingContext getRoutingContext() {
        return context;
    }
}
