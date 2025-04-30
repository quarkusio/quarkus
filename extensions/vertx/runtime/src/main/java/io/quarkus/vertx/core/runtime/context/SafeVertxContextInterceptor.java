package io.quarkus.vertx.core.runtime.context;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import org.jboss.logging.Logger;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.vertx.SafeVertxContext;
import io.vertx.core.Vertx;

@SafeVertxContext
@Priority(PLATFORM_BEFORE)
@Interceptor
public class SafeVertxContextInterceptor {

    @Inject
    Vertx vertx;

    private final static Logger LOGGER = Logger.getLogger(SafeVertxContextInterceptor.class);

    @AroundInvoke
    public Object markTheContextSafe(ArcInvocationContext ic) throws Exception {
        final io.vertx.core.Context current = vertx.getOrCreateContext();
        if (VertxContextSafetyToggle.isExplicitlyMarkedAsSafe(current)) {
            return ic.proceed();
        }

        var annotation = ic.findIterceptorBinding(SafeVertxContext.class);
        boolean unsafe = VertxContextSafetyToggle.isExplicitlyMarkedAsUnsafe(current);
        if (unsafe && annotation.force()) {
            LOGGER.debugf("Force the duplicated context as `safe` while is was explicitly marked as `unsafe` in %s.%s",
                    ic.getMethod().getDeclaringClass().getName(), ic.getMethod().getName());
        } else if (unsafe) {
            throw new IllegalStateException(
                    "Unable to mark the context as safe, as the current context is explicitly marked as unsafe");
        }
        VertxContextSafetyToggle.setContextSafe(current, true);
        return ic.proceed();
    }
}
