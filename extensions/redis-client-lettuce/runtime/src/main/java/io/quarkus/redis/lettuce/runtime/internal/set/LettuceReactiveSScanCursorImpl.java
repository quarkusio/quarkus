package io.quarkus.redis.lettuce.runtime.internal.set;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.function.Function;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.quarkus.redis.datasource.set.ReactiveSScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.AbstractReactiveScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionSource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveSScanCursorImpl<V> extends AbstractReactiveScanCursor<List<V>> implements ReactiveSScanCursor<V> {

    private final byte[] key;
    private final Function<List<byte[]>, List<V>> decoder;

    public LettuceReactiveSScanCursorImpl(LettuceConnectionSource source, byte[] key,
            Function<List<byte[]>, List<V>> decoder) {
        this(source, key, new ScanArgs(), decoder);
    }

    public LettuceReactiveSScanCursorImpl(LettuceConnectionSource source, byte[] key, ScanArgs scanArgs,
            Function<List<byte[]>, List<V>> decoder) {
        super(source, scanArgs);
        nonNull(key, "key");
        nonNull(decoder, "decoder");
        this.key = key;
        this.decoder = decoder;
    }

    @Override
    public Uni<List<V>> next() {
        // Reset cursor when finished to copy Vert.x behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(source, connection -> connection.async().sscan(key, current, scanArgs))
                .invoke(vc -> this.cursor = vc)
                .map(vc -> decoder.apply(vc.getValues()));
    }

    @Override
    public Multi<V> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(list -> Multi.createFrom().iterable(list));
    }

}
