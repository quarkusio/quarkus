package io.quarkus.redis.lettuce.runtime.internal.set;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.function.Function;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.async.RedisSetAsyncCommands;
import io.quarkus.redis.datasource.set.ReactiveSScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed {@link ReactiveSScanCursor}. Drives SSCAN, carrying the server cursor across
 * {@link #next()} calls until it wraps back to the initial position.
 *
 * @param <V> the member type
 */
public class LettuceReactiveSScanCursorImpl<V> implements ReactiveSScanCursor<V> {

    private final RedisSetAsyncCommands<byte[], byte[]> set;
    private final byte[] key;
    private final ScanArgs scanArgs;
    private final Function<List<byte[]>, List<V>> decoder;
    private ScanCursor cursor = ScanCursor.INITIAL;

    public LettuceReactiveSScanCursorImpl(RedisSetAsyncCommands<byte[], byte[]> set, byte[] key,
            Function<List<byte[]>, List<V>> decoder) {
        this(set, key, new ScanArgs(), decoder);
    }

    public LettuceReactiveSScanCursorImpl(RedisSetAsyncCommands<byte[], byte[]> set, byte[] key, ScanArgs scanArgs,
            Function<List<byte[]>, List<V>> decoder) {
        nonNull(set, "set");
        nonNull(key, "key");
        nonNull(scanArgs, "scanArgs");
        nonNull(decoder, "decoder");
        this.set = set;
        this.key = key;
        this.scanArgs = scanArgs;
        this.decoder = decoder;
    }

    @Override
    public boolean hasNext() {
        return !cursor.isFinished();
    }

    @Override
    public Uni<List<V>> next() {
        // Reset cursor when finished to copy Vert.x. behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(() -> set.sscan(key, current, scanArgs))
                .invoke(vc -> this.cursor = vc)
                .map(vc -> decoder.apply(vc.getValues()));
    }

    @Override
    public long cursorId() {
        return Long.parseUnsignedLong(cursor.getCursor());
    }

    @Override
    public Multi<V> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(list -> Multi.createFrom().iterable(list));
    }
}
