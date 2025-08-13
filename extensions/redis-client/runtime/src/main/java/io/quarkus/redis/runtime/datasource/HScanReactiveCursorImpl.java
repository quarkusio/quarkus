package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.hash.ReactiveHashScanCursor;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class HScanReactiveCursorImpl<F, V> extends AbstractRedisCommands implements ReactiveHashScanCursor<F, V> {

    private final byte[] key;
    private final Type typeOfField;
    private final Type typeOfValue;
    private long cursor;
    private boolean initial;
    private final List<String> extra = new ArrayList<>();

    public <K> HScanReactiveCursorImpl(RedisCommandExecutor redis, K key, Marshaller marshaller, Type typeOfField,
            Type typeOfValue,
            List<String> extra) {
        super(redis, marshaller);
        this.key = marshaller.encode(key);
        this.typeOfField = typeOfField;
        this.typeOfValue = typeOfValue;
        this.cursor = 0;
        this.initial = true;
        this.extra.addAll(extra);
    }

    @Override
    public boolean hasNext() {
        return initial || cursor != 0;
    }

    @Override
    public Uni<Map<F, V>> next() {
        initial = false;
        return execute(RedisCommand.of(Command.HSCAN).put(key).put(Long.toUnsignedString(cursor)).putAll(extra))
                .invoke(response -> cursor = Long.parseUnsignedLong(response.get(0).toString()))
                .map(response -> decode(response.get(1)));
    }

    public Multi<Map.Entry<F, V>> toMulti() {
        return Multi.createBy().repeating()
                .uni(this::next)
                .whilst(m -> hasNext())
                .onItem().transformToMultiAndConcatenate(map -> Multi.createFrom().items(map.entrySet().stream()));
    }

    public Map<F, V> decode(Response response) {
        Map<F, V> map = new LinkedHashMap<>();
        F field = null;
        for (Response member : response) {
            if (field == null) {
                field = marshaller.decode(typeOfField, member);
            } else {
                V val = marshaller.decode(typeOfValue, member);
                map.put(field, val);
                field = null;
            }
        }
        return map;
    }

    @Override
    public long cursorId() {
        return cursor;
    }
}
