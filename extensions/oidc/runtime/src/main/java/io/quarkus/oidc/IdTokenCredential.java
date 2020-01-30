package io.quarkus.oidc;

import io.quarkus.oidc.runtime.ContextAwareTokenCredential;
import io.vertx.ext.web.RoutingContext;

public class IdTokenCredential extends ContextAwareTokenCredential {

    public IdTokenCredential() {
        this(null, null);
    }

    public IdTokenCredential(String token, RoutingContext context) {
        super(token, "id_token", context);
    }
}
