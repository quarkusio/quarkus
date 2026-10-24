package io.quarkus.redis.lettuce.runtime.internal.hash;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Map;
import java.util.function.Function;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.async.RedisHashAsyncCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed {@link ReactiveHashScanCursor}. Drives HSCAN, carrying the server cursor across
 * {@link #next()} calls until it wraps back to the initial position.
 *
 * @param <F> the field type
 * @param <V> the value type
 */
public class LettuceReactiveHashScanCursorImpl<F, V> implements ReactiveHashScanCursor<F, V> {

    private final RedisHashAsyncCommands<byte[], byte[]> hash;
    private final byte[] key;
    private final ScanArgs scanArgs;
    private final Function<Map<byte[], byte[]>, Map<F, V>> decoder;
    private ScanCursor cursor = ScanCursor.INITIAL;

    public LettuceReactiveHashScanCursorImpl(RedisHashAsyncCommands<byte[], byte[]> hash, byte[] key,
            Function<Map<byte[], byte[]>, Map<F, V>> decoder) {
        this(hash, key, new ScanArgs(), decoder);
    }

    public LettuceReactiveHashScanCursorImpl(RedisHashAsyncCommands<byte[], byte[]> hash, byte[] key, ScanArgs scanArgs,
            Function<Map<byte[], byte[]>, Map<F, V>> decoder) {
        nonNull(hash, "hash");
        nonNull(key, "key");
        nonNull(scanArgs, "scanArgs");
        nonNull(decoder, "decoder");
        this.hash = hash;
        this.key = key;
        this.scanArgs = scanArgs;
        this.decoder = decoder;
    }

    @Override
    public boolean hasNext() {
        return !cursor.isFinished();
    }

    @Override
    public Uni<Map<F, V>> next() {
        // Reset cursor when finished to copy Vert.x. behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(() -> hash.hscan(key, current, scanArgs))
                .invoke(mc -> this.cursor = mc)
                .map(mc -> decoder.apply(mc.getMap()));
    }

    @Override
    public long cursorId() {
        return Long.parseUnsignedLong(cursor.getCursor());
    }

    @Override
    public Multi<Map.Entry<F, V>> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(m -> Multi.createFrom().iterable(m.entrySet()));
    }
}
