package io.quarkus.vertx.runtime;

import static io.quarkus.vertx.runtime.storage.QuarkusLocalStorageKeyVertxServiceProvider.REQUEST_SCOPED_LOCAL_KEY;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.RequestScoped;

import io.netty.util.concurrent.FastThreadLocal;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

public class VertxCurrentContextFactory implements CurrentContextFactory {

    private static final String LOCAL_KEY_PREFIX = "io.quarkus.vertx.cdi-current-context";

    private final List<String> keys;
    private final AtomicBoolean requestScopedKeyCreated;

    public VertxCurrentContextFactory() {
        // There will be only a few mutative operations max
        this.keys = new CopyOnWriteArrayList<>();
        this.requestScopedKeyCreated = new AtomicBoolean();
    }

    @Override
    public <T extends InjectableContext.ContextState> CurrentContext<T> create(Class<? extends Annotation> scope) {
        if (scope == RequestScoped.class) {
            if (!requestScopedKeyCreated.compareAndSet(false, true)) {
                throw new IllegalStateException(
                        "Multiple current contexts for the same scope are not supported. Current context for "
                                + scope + " already exists!");
            }
            return new VertxCurrentContext<T>(REQUEST_SCOPED_LOCAL_KEY);
        }
        String key = LOCAL_KEY_PREFIX + scope.getName();
        if (keys.contains(key)) {
            throw new IllegalStateException(
                    "Multiple current contexts for the same scope are not supported. Current context for "
                            + scope + " already exists!");
        }
        keys.add(key);
        return new VertxCurrentContext<>(key);
    }

    public ContextInternal duplicateContextIfContainsAnyCreatedScopeKeys(ContextInternal vertxContext) {
        if (!containsAnyCreatedScopeKeys(vertxContext)) {
            return vertxContext;
        }
        // Duplicate the context, copy the data, remove the request context
        var duplicateCtx = vertxContext.duplicate();
        // TODO this is not copying any ContextLocal<?> from the original context to the new one!
        var duplicateCtxData = duplicateCtx.localContextData();
        duplicateCtxData.putAll(vertxContext.localContextData());
        keys.forEach(duplicateCtxData::remove);
        if (requestScopedKeyCreated.get()) {
            duplicateCtx.removeLocal(REQUEST_SCOPED_LOCAL_KEY);
        }
        VertxContextSafetyToggle.setContextSafe(duplicateCtx, true);
        return duplicateCtx;
    }

    private boolean containsAnyCreatedScopeKeys(ContextInternal vertxContext) {
        boolean requestScopedKeyCreated = this.requestScopedKeyCreated.get();
        if (requestScopedKeyCreated && vertxContext.getLocal(REQUEST_SCOPED_LOCAL_KEY) != null) {
            return true;
        }
        if (keys.isEmpty()) {
            return false;
        }
        ConcurrentMap<Object, Object> local = vertxContext.localContextData();
        if (keys.size() == 1) {
            // Very often there will be only one key used
            return local.containsKey(keys.get(0));
        } else {
            for (String key : keys) {
                if (local.containsKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class VertxCurrentContext<T extends ContextState> implements CurrentContext<T> {

        // It allows to use both ContextLocalImpl and String keys
        private final Object key;
        private volatile FastThreadLocal<T> fallback;

        private VertxCurrentContext(Object key) {
            this.key = key;
        }

        @Override
        public T get() {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                return context.getLocal(key);
            }
            return fallback().get();
        }

        private FastThreadLocal<T> fallback() {
            var fallback = this.fallback;
            if (fallback == null) {
                fallback = getOrCreateFallback();
            }
            return fallback;
        }

        private synchronized FastThreadLocal<T> getOrCreateFallback() {
            var fallback = this.fallback;
            if (fallback == null) {
                fallback = new FastThreadLocal<>();
                this.fallback = fallback;
            }
            return fallback;
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
                fallback().set(state);
            }
        }

        @Override
        public void remove() {
            Context context = Vertx.currentContext();
            if (context != null && VertxContext.isDuplicatedContext(context)) {
                // NOOP - the DC should not be shared.
                // context.removeLocal(key);
            } else {
                fallback().remove();
            }
        }

    }
}
