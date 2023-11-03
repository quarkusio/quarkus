package io.quarkus.redis.datasource;

import io.smallrye.mutiny.Uni;

public interface ReactiveCursor<T> {

    /**
     * The cursor id when no operations have been emitted yet.
     */
    long INITIAL_CURSOR_ID = -1L;

    boolean hasNext();

    Uni<T> next();

    long cursorId();
}
