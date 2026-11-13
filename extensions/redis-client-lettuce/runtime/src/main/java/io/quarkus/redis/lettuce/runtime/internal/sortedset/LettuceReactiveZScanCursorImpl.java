package io.quarkus.redis.lettuce.runtime.internal.sortedset;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.function.Function;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.quarkus.redis.datasource.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.lettuce.runtime.internal.AbstractReactiveScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionSource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveZScanCursorImpl<V> extends AbstractReactiveScanCursor<List<ScoredValue<V>>>
        implements ReactiveZScanCursor<V> {

    private final byte[] key;
    private final Function<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> decoder;

    public LettuceReactiveZScanCursorImpl(LettuceConnectionSource source, byte[] key,
            Function<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> decoder) {
        this(source, key, new ScanArgs(), decoder);
    }

    public LettuceReactiveZScanCursorImpl(LettuceConnectionSource source, byte[] key,
            ScanArgs scanArgs, Function<List<io.lettuce.core.ScoredValue<byte[]>>, List<ScoredValue<V>>> decoder) {
        super(source, scanArgs);
        nonNull(key, "key");
        nonNull(decoder, "decoder");
        this.key = key;
        this.decoder = decoder;
    }

    @Override
    public Uni<List<ScoredValue<V>>> next() {
        // Reset cursor when finished to copy Vert.x behavior.
        final ScanCursor current = cursor.isFinished() ? ScanCursor.INITIAL : cursor;
        return LettuceResult.toUni(source, connection -> connection.async().zscan(key, current, scanArgs))
                .invoke(sc -> this.cursor = sc)
                .map(sc -> decoder.apply(sc.getValues()));
    }

    @Override
    public Multi<ScoredValue<V>> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(list -> Multi.createFrom().iterable(list));
    }

}
