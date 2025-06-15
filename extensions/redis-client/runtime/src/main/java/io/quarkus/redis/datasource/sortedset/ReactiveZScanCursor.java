package io.quarkus.redis.datasource.sortedset;

import java.util.List;

import io.quarkus.redis.datasource.ReactiveCursor;
import io.smallrye.mutiny.Multi;

public interface ReactiveZScanCursor<V> extends ReactiveCursor<List<ScoredValue<V>>> {

    /**
     * Produces a {@code Multi} emitting each member from the sorted set individually. Unlike {@link #next()} which
     * provides the members by batch, this method returns them one by one.
     *
     * @return the multi
     */
    Multi<ScoredValue<V>> toMulti();

}
