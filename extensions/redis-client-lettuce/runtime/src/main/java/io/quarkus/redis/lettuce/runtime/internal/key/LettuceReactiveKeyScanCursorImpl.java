package io.quarkus.redis.lettuce.runtime.internal.key;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.lettuce.core.KeyScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed {@link ReactiveKeyScanCursor}. Drives SCAN, carrying the server cursor across
 * {@link #next()} calls until it wraps back to the initial position.
 *
 * @param <K> the key type
 */
public class LettuceReactiveKeyScanCursorImpl<K> implements ReactiveKeyScanCursor<K> {

    private final RedisClusterAsyncCommands<byte[], byte[]> async;
    private final KeyScanArgs keyScanArgs;
    private final Function<List<byte[]>, List<K>> decoder;
    private ScanCursor cursor = ScanCursor.INITIAL;

    public LettuceReactiveKeyScanCursorImpl(RedisClusterAsyncCommands<byte[], byte[]> async,
            Function<List<byte[]>, List<K>> decoder) {
        this(async, new KeyScanArgs(), decoder);
    }

    public LettuceReactiveKeyScanCursorImpl(RedisClusterAsyncCommands<byte[], byte[]> async, KeyScanArgs keyScanArgs,
            Function<List<byte[]>, List<K>> decoder) {
        nonNull(async, "async");
        nonNull(keyScanArgs, "args");
        nonNull(decoder, "decoder");
        this.async = async;
        this.keyScanArgs = keyScanArgs;
        this.decoder = decoder;
    }

    @Override
    public boolean hasNext() {
        return !cursor.isFinished();
    }

    @Override
    public Uni<Set<K>> next() {
        // Reset cursor when finished to copy Vert.x. behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(() -> async.scan(current, keyScanArgs))
                .invoke(kc -> this.cursor = kc)
                .map(kc -> Collections.unmodifiableSet(new LinkedHashSet<>(decoder.apply(kc.getKeys()))));
    }

    @Override
    public long cursorId() {
        return Long.parseUnsignedLong(cursor.getCursor());
    }

    @Override
    public Multi<K> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(set -> hasNext())
                .onItem().transformToMultiAndConcatenate(set -> Multi.createFrom().iterable(set));
    }
}
