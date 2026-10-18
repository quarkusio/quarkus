package io.quarkus.redis.datasource;

import io.smallrye.mutiny.Uni;

public interface ReactiveCursor<T> {

    /**
     * The cursor id when no operations have been emitted yet.
     *
     * @deprecated Previously, this value was -1, assuming that this value is never produced by Redis as an actual cursor.
     *             However, Redis uses unsigned 64-bit integers as cursor values, so the assumption was wrong
     *             (-1 as 64-bit signed integer is 0xFFFF_FFFF_FFFF_FFFF, which is the biggest unsigned 64-bit integer).
     *             This should have never been exposed publicly and should not be relied upon.
     *             <p>
     *             The current value is 0, which is the correct initial <em>and</em> final cursor value in Redis.
     */
    @Deprecated(forRemoval = true, since = "3.26")
    long INITIAL_CURSOR_ID = 0L;

    boolean hasNext();

    Uni<T> next();

    /**
     * @deprecated see {@link #INITIAL_CURSOR_ID}
     */
    @Deprecated(forRemoval = true, since = "3.26")
    long cursorId();
}
