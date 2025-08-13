package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
    private boolean initial;
    private final List<String> extra = new ArrayList<>();

    public <K> SScanReactiveCursorImpl(RedisCommandExecutor redis, K key, Marshaller marshaller,
            Type typeOfValue, List<String> extra) {
        super(redis, marshaller);
        this.key = marshaller.encode(key);
        this.cursor = 0;
        this.initial = true;
        this.marshaller = marshaller;
        this.typeOfValue = typeOfValue;
        this.extra.addAll(extra);
    }

    @Override
    public boolean hasNext() {
        return initial || cursor != 0;
    }

    @Override
    public Uni<List<V>> next() {
        initial = false;
        return execute(RedisCommand.of(Command.SSCAN).put(key).put(Long.toUnsignedString(cursor)).putAll(extra))
                .invoke(response -> cursor = Long.parseUnsignedLong(response.get(0).toString()))
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
