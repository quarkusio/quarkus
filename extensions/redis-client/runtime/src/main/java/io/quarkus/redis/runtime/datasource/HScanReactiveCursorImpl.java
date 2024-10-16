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
    private final List<String> extra = new ArrayList<>();

    public <K> HScanReactiveCursorImpl(RedisCommandExecutor redis, K key, Marshaller marshaller, Type typeOfField,
            Type typeOfValue,
            List<String> extra) {
        super(redis, marshaller);
        this.key = marshaller.encode(key);
        this.typeOfField = typeOfField;
        this.typeOfValue = typeOfValue;
        this.cursor = INITIAL_CURSOR_ID;
        this.extra.addAll(extra);
    }

    @Override
    public boolean hasNext() {
        return cursor != 0;
    }

    @Override
    public Uni<Map<F, V>> next() {
        long pos = cursor == INITIAL_CURSOR_ID ? 0 : cursor;
        RedisCommand cmd = RedisCommand.of(Command.HSCAN);
        cmd.put(key);
        cmd.put(pos);
        cmd.putAll(extra);
        return execute(cmd)
                .invoke(response -> cursor = response.get(0).toLong())
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
