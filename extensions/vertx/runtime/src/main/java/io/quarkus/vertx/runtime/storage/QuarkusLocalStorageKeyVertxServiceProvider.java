package io.quarkus.vertx.runtime.storage;

import io.quarkus.arc.InjectableContext;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.context.storage.ContextLocal;

/**
 * This provider exists with the sole purpose of reliably get the optimized local keys created before
 * the Vertx instance is created.
 */
public class QuarkusLocalStorageKeyVertxServiceProvider implements VertxServiceProvider {

    public static final ContextLocal<Boolean> ACCESS_TOGGLE_KEY = VertxContextSafetyToggle.registerAccessToggleKey();
    public static final ContextLocal<InjectableContext.ContextState> REQUEST_SCOPED_LOCAL_KEY = ContextLocal
            .registerLocal(InjectableContext.ContextState.class);

    @Override
    public void init(VertxBuilder builder) {

    }
}
