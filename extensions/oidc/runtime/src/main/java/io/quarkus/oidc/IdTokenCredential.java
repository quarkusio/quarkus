package io.quarkus.oidc;

import io.vertx.ext.web.RoutingContext;

public class IdTokenCredential extends OidcTokenCredential {
    private final boolean internal;

    public IdTokenCredential() {
        this(null, null);
    }

    public IdTokenCredential(String token, RoutingContext context) {
        this(token, context, false);
    }

    public IdTokenCredential(String token, RoutingContext context, boolean internal) {
        super(token, "id_token", context);
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }
}
