package io.quarkus.http.ratelimit.runtime.internal.store;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.faulttolerance.api.Guard;

/**
 * A guard without strategies, for tests of what {@link GuardedRateLimitStore} does around the guard.
 */
final class PassThroughGuard implements Guard {

    @Override
    public <T> T call(Callable<T> action, Class<T> type) throws Exception {
        return action.call();
    }

    @Override
    public <T> T call(Callable<T> action, TypeLiteral<T> type) throws Exception {
        return action.call();
    }

    @Override
    public <T> T get(Supplier<T> action, Class<T> type) {
        return action.get();
    }

    @Override
    public <T> T get(Supplier<T> action, TypeLiteral<T> type) {
        return action.get();
    }
}
