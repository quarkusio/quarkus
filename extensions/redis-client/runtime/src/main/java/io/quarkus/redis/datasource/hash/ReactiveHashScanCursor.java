package io.quarkus.redis.datasource.hash;

import java.util.Map;

import io.quarkus.redis.datasource.ReactiveCursor;
import io.smallrye.mutiny.Multi;

public interface ReactiveHashScanCursor<K, V> extends ReactiveCursor<Map<K, V>> {

    /**
     * Produces a {@code Multi} emitting each entry from hash individually. Unlike {@link #next()} which provides the
     * entries by batch, this method returns them one by one.
     *
     * @return the multi
     */
    Multi<Map.Entry<K, V>> toMulti();

}
