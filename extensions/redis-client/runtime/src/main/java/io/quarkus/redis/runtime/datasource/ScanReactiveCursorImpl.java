package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.keys.ReactiveKeyScanCursor;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;

public class ScanReactiveCursorImpl<K> extends AbstractRedisCommands implements ReactiveKeyScanCursor<K> {

    private final Type typeOfKey;
    private long cursor;
    private boolean initial;
    private final List<String> extra = new ArrayList<>();

    public ScanReactiveCursorImpl(RedisCommandExecutor redis, Marshaller marshaller, Type typeOfKey, List<String> extra) {
        super(redis, marshaller);
        this.cursor = 0;
        this.initial = true;
        this.typeOfKey = typeOfKey;
        this.extra.addAll(extra);
    }

    @Override
    public long cursorId() {
        return cursor;
    }

    @Override
    public boolean hasNext() {
        return initial || cursor != 0;
    }

    @Override
    public Uni<Set<K>> next() {
        initial = false;
        return execute(RedisCommand.of(Command.SCAN).put(Long.toUnsignedString(cursor)).putAll(extra))
                .invoke(response -> cursor = Long.parseUnsignedLong(response.get(0).toString()))
                .map(response -> marshaller.decodeAsSet(response.get(1), typeOfKey));
    }

    @Override
    public Multi<K> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(set -> Multi.createFrom().items(set.stream()));
    }
}
