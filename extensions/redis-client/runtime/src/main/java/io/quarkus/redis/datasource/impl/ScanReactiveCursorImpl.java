package io.quarkus.redis.datasource.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.api.ReactiveCursor;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;

public class ScanReactiveCursorImpl<K> extends AbstractRedisCommands implements ReactiveCursor<Set<K>> {

    private final Class<K> typeOfKey;
    private long cursor;
    private final List<String> extra = new ArrayList<>();

    public ScanReactiveCursorImpl(RedisCommandExecutor redis, Marshaller marshaller, Class<K> typeOfKey, List<String> extra) {
        super(redis, marshaller);
        this.cursor = ReactiveCursor.INITIAL_CURSOR_ID;
        this.typeOfKey = typeOfKey;
        this.extra.addAll(extra);
    }

    @Override
    public long cursorId() {
        return cursor;
    }

    @Override
    public boolean hasNext() {
        return cursor != 0;
    }

    @Override
    public Uni<Set<K>> next() {
        long pos = cursor == INITIAL_CURSOR_ID ? 0 : cursor;
        return execute(RedisCommand.of(Command.SCAN).put(pos).putAll(extra))
                .invoke(response -> cursor = response.get(0).toLong())
                .map(response -> marshaller.decodeAsSet(response.get(1), typeOfKey));
    }
}
