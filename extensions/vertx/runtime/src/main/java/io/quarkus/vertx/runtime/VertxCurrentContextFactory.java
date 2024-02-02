package io.quarkus.vertx.runtime;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private static final String LOCAL_KEY_PREFIX = "io.quarkus.vertx.cdi-current-context";

    private final List<String> keys;
    private final List<String> unmodifiableKeys;

    public VertxCurrentContextFactory() {
        // There will be only a few mutative operations max
        this.keys = new CopyOnWriteArrayList<>();
        // We do not want to allocate a new object for each VertxCurrentContextFactory#keys() invocation
        this.unmodifiableKeys = Collections.unmodifiableList(keys);
    }

    @Override
    public <T extends InjectableContext.ContextState> CurrentContext<T> create(Class<? extends Annotation> scope) {
        String key = LOCAL_KEY_PREFIX + scope.getName();
        if (keys.contains(key)) {
            throw new IllegalStateException(
                    "Multiple current contexts for the same scope are not supported. Current context for "
                            + scope + " already exists!");
        }
        keys.add(key);
        return new VertxCurrentContext<>(key);
    }

    /**
     *
     * @return an unmodifiable list of used keys
     */
    public List<String> keys() {
        return unmodifiableKeys;
    }

    private static final class VertxCurrentContext<T extends ContextState> implements CurrentContext<T> {

        private final String key;
        private final FastThreadLocal<T> fallback = new FastThreadLocal<>();

        private VertxCurrentContext(String key) {
            this.key = key;
        }

        @Override
        public T get() {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                return context.getLocal(key);
            }
            return fallback.get();
        }

        @Override
        public void set(T state) {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                VertxContextSafetyToggle.setContextSafe(context, true);
                // this is racy but should be fine, because DC should not be shared
                // and never remove the existing mapping
                var oldState = context.getLocal(key);
                if (oldState != state) {
                    context.putLocal(key, state);
                }

            } else {
                fallback.set(state);
            }
        }

        @Override
        public void remove() {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                // NOOP - the DC should not be shared.
                // context.removeLocal(key);
            } else {
                fallback.remove();
            }
        }

    }
}
