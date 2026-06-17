package io.quarkus.smallrye.graphql.runtime;

import io.vertx.core.Context;
import io.vertx.core.impl.TaskQueue;

/**
 * Installs and retrieves a per-request {@link TaskQueue} on a Vert.x {@link Context}'s local map.
 * This allows us to preserve ordering, and scope it to the request so resolvers from different requests can run in parallel
 */
public final class RequestScopedTaskQueue {

    static final String LOCAL_KEY = RequestScopedTaskQueue.class.getName();

    private RequestScopedTaskQueue() {
    }

    /**
     * Returns the {@link TaskQueue} already installed on the given context, or installs and
     * returns a fresh one if none was present. Safe to call multiple times for the same request.
     *
     * @param vc the (typically per-request duplicated) Vert.x context; may be {@code null}
     * @return the request-scoped {@link TaskQueue}, or {@code null} if {@code vc} was {@code null}
     */
    public static TaskQueue installIfAbsent(Context vc) {
        if (vc == null) {
            return null;
        }
        TaskQueue existing = vc.getLocal(LOCAL_KEY);
        if (existing != null) {
            return existing;
        }
        TaskQueue queue = new TaskQueue();
        vc.putLocal(LOCAL_KEY, queue);
        return queue;
    }

    /**
     * Returns the {@link TaskQueue} previously installed on the given context, or {@code null}
     * if none has been installed (for example, when called outside of a GraphQL HTTP request).
     */
    public static TaskQueue get(Context vc) {
        if (vc == null) {
            return null;
        }
        return vc.getLocal(LOCAL_KEY);
    }
}
