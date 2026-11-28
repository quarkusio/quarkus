package io.quarkus.redis.lettuce.runtime.internal.key;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.lettuce.core.KeyScanArgs;
import io.lettuce.core.ScanCursor;
import io.quarkus.redis.datasource.keys.ReactiveKeyScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.AbstractReactiveScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionSource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveKeyScanCursorImpl<K> extends AbstractReactiveScanCursor<Set<K>>
        implements ReactiveKeyScanCursor<K> {

    private final Function<List<byte[]>, Set<K>> decoder;

    public LettuceReactiveKeyScanCursorImpl(LettuceConnectionSource source,
            Function<List<byte[]>, Set<K>> decoder) {
        this(source, new KeyScanArgs(), decoder);
    }

    public LettuceReactiveKeyScanCursorImpl(LettuceConnectionSource source, KeyScanArgs scanArgs,
            Function<List<byte[]>, Set<K>> decoder) {
        super(source, scanArgs);
        nonNull(decoder, "decoder");
        this.decoder = decoder;
    }

    @Override
    public Uni<Set<K>> next() {
        // Reset cursor when finished to copy Vert.x behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(source, connection -> connection.async().scan(current, scanArgs))
                .invoke(kc -> this.cursor = kc)
                .map(kc -> decoder.apply(kc.getKeys()));
    }

    @Override
    public Multi<K> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(set -> hasNext())
                .onItem().transformToMultiAndConcatenate(set -> Multi.createFrom().iterable(set));
    }

}
