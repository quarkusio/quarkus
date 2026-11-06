package io.quarkus.redis.lettuce.runtime.internal.sortedset;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.function.Function;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.async.RedisSortedSetAsyncCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed {@link ReactiveZScanCursor}. Drives ZSCAN, carrying the server cursor across
 * {@link #next()} calls until it wraps back to the initial position.
 *
 * @param <V> the type of the scored member
 */
public class LettuceReactiveZScanCursorImpl<V> implements ReactiveZScanCursor<V> {

    private final RedisSortedSetAsyncCommands<byte[], byte[]> sortedSet;
    private final byte[] key;
    private final ScanArgs scanArgs;
    private final Function<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> decoder;
    private ScanCursor cursor = ScanCursor.INITIAL;

    public LettuceReactiveZScanCursorImpl(RedisSortedSetAsyncCommands<byte[], byte[]> sortedSet, byte[] key,
            Function<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> decoder) {
        this(sortedSet, key, new ScanArgs(), decoder);
    }

    public LettuceReactiveZScanCursorImpl(RedisSortedSetAsyncCommands<byte[], byte[]> sortedSet, byte[] key,
            ScanArgs scanArgs, Function<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> decoder) {
        nonNull(sortedSet, "sortedSet");
        nonNull(key, "key");
        nonNull(scanArgs, "scanArgs");
        nonNull(decoder, "decoder");
        this.sortedSet = sortedSet;
        this.key = key;
        this.scanArgs = scanArgs;
        this.decoder = decoder;
    }

    @Override
    public boolean hasNext() {
        return !cursor.isFinished();
    }

    @Override
    public Uni<List<ScoredValue<V>>> next() {
        // Reset cursor when finished to copy Vert.x. behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(() -> sortedSet.zscan(key, current, scanArgs))
                .invoke(sc -> this.cursor = sc)
                .map(sc -> decoder.apply(sc.getValues()));
    }

    @Override
    public long cursorId() {
        return Long.parseUnsignedLong(cursor.getCursor());
    }

    @Override
    public Multi<ScoredValue<V>> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(list -> Multi.createFrom().iterable(list));
    }
}
