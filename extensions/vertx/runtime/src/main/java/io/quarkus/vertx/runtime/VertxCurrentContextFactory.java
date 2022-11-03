package io.quarkus.vertx.runtime;

import java.lang.annotation.Annotation;

import io.netty.util.concurrent.FastThreadLocal;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class VertxCurrentContextFactory implements CurrentContextFactory {

    public static final String LOCAL_KEY = "io.quarkus.vertx.cdi-current-context";

    @Override
    public <T extends InjectableContext.ContextState> CurrentContext<T> create(Class<? extends Annotation> scope) {
        return new VertxCurrentContext<>();
    }

    private static final class VertxCurrentContext<T extends ContextState> implements CurrentContext<T> {

        private final FastThreadLocal<T> fallback = new FastThreadLocal<>();

        @Override
        public T get() {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                return context.getLocal(LOCAL_KEY);
            }
            return fallback.get();
        }

        @Override
        public void set(T state) {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                VertxContextSafetyToggle.setContextSafe(context, true);
                context.putLocal(LOCAL_KEY, state);
            } else {
                fallback.set(state);
            }
        }

        @Override
        public void remove() {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                // NOOP - the DC should not be shared.
                // context.removeLocal(LOCAL_KEY);
            } else {
                fallback.remove();
            }
        }

    }
}
