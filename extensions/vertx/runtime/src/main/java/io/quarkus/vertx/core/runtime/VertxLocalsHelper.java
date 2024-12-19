package io.quarkus.vertx.core.runtime;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.ContextLocalImpl;
import io.vertx.core.spi.context.storage.ContextLocal;

public class VertxLocalsHelper {

    private static final String ILLEGAL_ACCESS_TO_CONTEXT = "Access to Context.put(), Context.get() and Context.remove() " +
            "are forbidden as it can leak data between unrelated processing. Use Context.putLocal(), Context.getLocal() " +
            "and Context.removeLocal() instead. Note that these methods can only be used from a 'duplicated' Context, " +
            "and so may not be available everywhere.";

    private static final String ILLEGAL_ACCESS_TO_LOCAL_CONTEXT = "Access to Context.putLocal(), Context.getLocal() and" +
            " Context.removeLocal() are forbidden from a 'root' context  as it can leak data between unrelated processing." +
            " Make sure the method runs on a 'duplicated' (local) Context";

    public static void throwOnRootContextAccess() {
        throw new UnsupportedOperationException(ILLEGAL_ACCESS_TO_CONTEXT);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getLocal(ContextInternal context, Object key) {
        if (VertxContext.isDuplicatedContext(context)) {
            // We are on a duplicated context, allow accessing the locals
            if (key instanceof ContextLocalImpl<?>) {
                var localKey = (ContextLocal<T>) key;
                return (T) context.getLocal(localKey, QuarkusAccessModes.ACQUIRE_RELEASE);
            }
            assert !(key instanceof ContextLocal<?>);
            return (T) context.localContextData().get(key);
        } else {
            throw new UnsupportedOperationException(ILLEGAL_ACCESS_TO_LOCAL_CONTEXT);
        }
    }

    public static void putLocal(ContextInternal context, Object key, Object value) {
        if (VertxContext.isDuplicatedContext(context)) {
            // We are on a duplicated context, allow accessing the locals
            if (key instanceof ContextLocalImpl<?>) {
                var localKey = (ContextLocal<Object>) key;
                context.putLocal(localKey, QuarkusAccessModes.ACQUIRE_RELEASE, value);
            } else {
                assert !(key instanceof ContextLocal<?>);
                context.localContextData().put(key, value);
            }
        } else {
            throw new UnsupportedOperationException(ILLEGAL_ACCESS_TO_LOCAL_CONTEXT);
        }
    }

    public static boolean removeLocal(ContextInternal context, Object key) {
        if (VertxContext.isDuplicatedContext(context)) {
            // We are on a duplicated context, allow accessing the locals
            if (key instanceof ContextLocalImpl<?>) {
                var localKey = (ContextLocal<Object>) key;
                if (localKey == null) {
                    return false;
                }
                context.removeLocal(localKey, QuarkusAccessModes.ACQUIRE_RELEASE);
                return true;
            }
            assert !(key instanceof ContextLocal<?>);
            return context.localContextData().remove(key) != null;
        } else {
            throw new UnsupportedOperationException(ILLEGAL_ACCESS_TO_LOCAL_CONTEXT);
        }
    }
}
