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
     *
     * @deprecated Use {@link #addSpecialMethodHandler(CacheSpecialMethodHandler)} from a service instead.
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public void registerSpecialMethodHandler(CacheSpecialMethodHandler handler) {
        addSpecialMethodHandler(handler);
    }

    /**
     * Registers a {@link CacheSpecialMethodHandler}. Companion extensions call this from a service.
     */
    public static void addSpecialMethodHandler(CacheSpecialMethodHandler handler) {
        specialMethodHandlers.add(Objects.requireNonNull(handler));
    }

    /**
     * Removes a {@link CacheSpecialMethodHandler} added by {@link #addSpecialMethodHandler(CacheSpecialMethodHandler)}.
     */
    public static void removeSpecialMethodHandler(CacheSpecialMethodHandler handler) {
        specialMethodHandlers.remove(handler);
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
