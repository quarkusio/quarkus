package io.quarkus.redis.datasource.keys;

import java.util.Set;

import io.quarkus.redis.datasource.ReactiveCursor;
import io.smallrye.mutiny.Multi;

public interface ReactiveKeyScanCursor<K> extends ReactiveCursor<Set<K>> {

    /**
     * Produces a {@code Multi} emitting each key individually. Unlike {@link #next()} which provides the keys by batch,
     * this method returns them one by one.
     *
     * @return the multi
     */
    Multi<K> toMulti();

}
