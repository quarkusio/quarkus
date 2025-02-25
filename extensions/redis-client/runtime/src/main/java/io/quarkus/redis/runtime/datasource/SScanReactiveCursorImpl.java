package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveCursor;
import io.quarkus.redis.datasource.set.ReactiveSScanCursor;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class SScanReactiveCursorImpl<V> extends AbstractRedisCommands implements ReactiveSScanCursor<V> {

    private final byte[] key;
    private final Type typeOfValue;
    private final Marshaller marshaller;
    private long cursor;
    private final List<String> extra = new ArrayList<>();

    public <K> SScanReactiveCursorImpl(RedisCommandExecutor redis, K key, Marshaller marshaller,
            Type typeOfValue, List<String> extra) {
        super(redis, marshaller);
        this.key = marshaller.encode(key);
        this.cursor = ReactiveCursor.INITIAL_CURSOR_ID;
        this.marshaller = marshaller;
        this.typeOfValue = typeOfValue;
        this.extra.addAll(extra);
    }

    @Override
    public boolean hasNext() {
        return cursor != 0;
    }

    @Override
    public Uni<List<V>> next() {
        long pos = cursor == INITIAL_CURSOR_ID ? 0 : cursor;
        RedisCommand cmd = RedisCommand.of(Command.SSCAN);
        cmd.put(key);
        cmd.put(Long.toString(pos));
        cmd.putAll(extra);
        return execute(cmd)
                .invoke(response -> cursor = response.get(0).toLong())
                .map(response -> {
                    Response array = response.get(1);
                    List<V> list = new ArrayList<>();
                    for (Response nested : array) {
                        list.add(marshaller.decode(typeOfValue, nested.toBytes()));
                    }
                    return list;
                });
    }

    @Override
    public long cursorId() {
        return cursor;
    }

    @Override
    public Multi<V> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(list -> Multi.createFrom().items(list.stream()));
    }
}
