package io.quarkus.redis.datasource;

public interface Cursor<T> {

    /**
     * The cursor id when no operations have been emitted yet.
     */
    long INITIAL_CURSOR_ID = ReactiveCursor.INITIAL_CURSOR_ID;

    boolean hasNext();

    T next();

    long cursorId();
}
