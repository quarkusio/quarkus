package io.quarkus.cache.runtime;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Holds runtime registrations for cache-related special method handling contributed by other extensions.
 */
@Recorder
public class CacheRecorder {

    private static final List<CacheSpecialMethodHandler> specialMethodHandlers = new CopyOnWriteArrayList<>();

    /**
     * Registers a {@link CacheSpecialMethodHandler}. Companion extensions call this from a recorded build step.
     */
    public void registerSpecialMethodHandler(CacheSpecialMethodHandler handler) {
        specialMethodHandlers.add(Objects.requireNonNull(handler));
    }

    /**
     * Returns the first registered handler that can handle the given method, or {@code null} if none matches.
     */
    public static CacheSpecialMethodHandler getSpecialMethodHandler(Method method) {
        for (CacheSpecialMethodHandler handler : specialMethodHandlers) {
            if (handler.canHandle(method)) {
                return handler;
            }
        }
        return null;
    }
}
