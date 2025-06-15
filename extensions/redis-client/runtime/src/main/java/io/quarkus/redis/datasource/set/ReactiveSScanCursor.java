package io.quarkus.redis.datasource.set;

import java.util.List;

import io.quarkus.redis.datasource.ReactiveCursor;
import io.smallrye.mutiny.Multi;

public interface ReactiveSScanCursor<V> extends ReactiveCursor<List<V>> {

    /**
     * Returns an {@code Multi} providing each member of the set individually. Unlike {@link #next()} which provides the
     * members by batch, this method returns them one by one.
     *
     * @return the multi
     */
    Multi<V> toMulti();

}
