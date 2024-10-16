package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class ZScanReactiveCursorImpl<V> extends AbstractRedisCommands implements ReactiveZScanCursor<V> {

    private final byte[] key;
    private final Type typeOfValue;
    private long cursor;
    private final List<String> extra = new ArrayList<>();

    public <K> ZScanReactiveCursorImpl(RedisCommandExecutor redis, K key, Marshaller marshaller, Type typeOfValue,
            List<String> extra) {
        super(redis, marshaller);
        this.key = marshaller.encode(key);
        this.cursor = INITIAL_CURSOR_ID;
        this.typeOfValue = typeOfValue;
        this.extra.addAll(extra);
    }

    @Override
    public boolean hasNext() {
        return cursor != 0;
    }

    @Override
    public Uni<List<ScoredValue<V>>> next() {
        RedisCommand cmd = RedisCommand.of(Command.ZSCAN);
        long pos = cursor == INITIAL_CURSOR_ID ? 0 : cursor;
        cmd.put(key);
        cmd.put(Long.toString(pos));
        cmd.putAll(extra);
        return execute(cmd)
                .invoke(response -> cursor = response.get(0).toLong())
                .map(response -> {
                    Response array = response.get(1);
                    V value = null;
                    List<ScoredValue<V>> list = new ArrayList<>();
                    for (Response nested : array) {
                        if (value == null) {
                            value = marshaller.decode(typeOfValue, nested.toBytes());
                        } else {
                            list.add(new ScoredValue<>(value, nested.toDouble()));
                            value = null;
                        }
                    }
                    return list;
                });
    }

    @Override
    public long cursorId() {
        return cursor;
    }

    @Override
    public Multi<ScoredValue<V>> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(list -> Multi.createFrom().items(list.stream()));
    }
}
