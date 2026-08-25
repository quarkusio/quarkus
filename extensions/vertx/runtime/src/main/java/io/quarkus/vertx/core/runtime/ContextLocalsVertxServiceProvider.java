package io.quarkus.vertx.core.runtime;

import io.quarkus.security.credential.TokenCredential;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.context.storage.ContextLocal;

// Registers Quarkus-level ContextLocal instances and triggers SmallRye VertxContext static initialization.
public class ContextLocalsVertxServiceProvider implements VertxServiceProvider {

    public static final ContextLocal<TokenCredential> TOKEN_CREDENTIAL_LOCAL = ContextLocal
            .registerLocal(TokenCredential.class);

    @Override
    public void init(VertxBootstrap builder) {
        // Just touch the class.
        VertxContext.isOnDuplicatedContext();
    }
}
