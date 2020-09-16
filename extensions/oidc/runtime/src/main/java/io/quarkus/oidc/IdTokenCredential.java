package io.quarkus.oidc;

import io.vertx.ext.web.RoutingContext;

public class IdTokenCredential extends OidcTokenCredential {

    public IdTokenCredential() {
        this(null, null);
    }

    public IdTokenCredential(String token, RoutingContext context) {
        super(token, "id_token", context);
    }
}
