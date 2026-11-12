package io.quarkus.redis.lettuce.runtime.internal.hash;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Map;
import java.util.function.Function;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.quarkus.redis.datasource.hash.ReactiveHashScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.AbstractReactiveScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionSource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveHashScanCursorImpl<F, V> extends AbstractReactiveScanCursor<Map<F, V>>
        implements ReactiveHashScanCursor<F, V> {

    private final byte[] key;
    private final Function<Map<byte[], byte[]>, Map<F, V>> decoder;

    public LettuceReactiveHashScanCursorImpl(LettuceConnectionSource source, byte[] key,
            Function<Map<byte[], byte[]>, Map<F, V>> decoder) {
        this(source, key, new ScanArgs(), decoder);
    }

    public LettuceReactiveHashScanCursorImpl(LettuceConnectionSource source, byte[] key, ScanArgs scanArgs,
            Function<Map<byte[], byte[]>, Map<F, V>> decoder) {
        super(source, scanArgs);
        nonNull(key, "key");
        nonNull(decoder, "decoder");
        this.key = key;
        this.decoder = decoder;
    }

    @Override
    public Uni<Map<F, V>> next() {
        // Reset cursor when finished to copy Vert.x behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(source, connection -> connection.async().hscan(key, current, scanArgs))
                .invoke(mc -> this.cursor = mc)
                .map(mc -> decoder.apply(mc.getMap()));
    }

    @Override
    public Multi<Map.Entry<F, V>> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(m -> Multi.createFrom().iterable(m.entrySet()));
    }

}
