package io.quarkus.redis.lettuce.runtime.internal.topk;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.probabilistic.TopKListValue;
import io.lettuce.core.probabilistic.arguments.TopKReserveArgs;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveTopKCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveTopKCommands<K, V> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveTopKCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<V> topkAdd(K key, V item) {
        return _topkAdd(key, item).toUni();
    }

    LettuceCommand<List<String>, V> _topkAdd(K key, V item) {
        nonNull(key, "key");
        nonNull(item, "item");
        return LettuceCommand.of(() -> async.topKAdd(marshaller.encode(key), marshaller.encode(item)),
                list -> decodeValue(list.getFirst()));
    }

    @SafeVarargs
    @Override
    public final Uni<List<V>> topkAdd(K key, V... items) {
        return _topkAdd(key, items).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<String>, List<V>> _topkAdd(K key, V... items) {
        nonNull(key, "key");
        doesNotContainNull(items, "items");
        if (items.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`items` must not be empty"));
        }
        return LettuceCommand.of(() -> async.topKAdd(marshaller.encode(key), marshaller.encodeAsArray(items)),
                reply -> decodeList(reply, this::decodeValue));
    }

    @Override
    public Uni<V> topkIncrBy(K key, V item, int increment) {
        return _topkIncrBy(key, item, increment).toUni();
    }

    LettuceCommand<List<String>, V> _topkIncrBy(K key, V item, int increment) {
        nonNull(key, "key");
        nonNull(item, "item");
        positive(increment, "increment");
        return LettuceCommand.of(() -> async.topKIncrBy(marshaller.encode(key), marshaller.encode(item), increment),
                list -> decodeValue(list.getFirst()));
    }

    @Override
    public Uni<Map<V, V>> topkIncrBy(K key, Map<V, Integer> couples) {
        return _topkIncrBy(key, couples).toUni();
    }

    LettuceCommand<List<String>, Map<V, V>> _topkIncrBy(K key, Map<V, Integer> couples) {
        nonNull(key, "key");
        nonNull(couples, "couples");
        if (couples.isEmpty()) {
            return LettuceCommand.failing(new IllegalArgumentException("`couples` must not be empty"));
        }
        IncrementPair<byte[]>[] pairs = encodeCouples(couples);
        List<V> items = List.copyOf(couples.keySet());
        return LettuceCommand.of(() -> async.topKIncrBy(marshaller.encode(key), pairs),
                dropped -> zipDropped(items, dropped));
    }

    @Override
    public Uni<List<V>> topkList(K key) {
        return _topkList(key).toUni();
    }

    LettuceCommand<List<String>, List<V>> _topkList(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.topKList(marshaller.encode(key)), reply -> decodeList(reply, this::decodeValue));
    }

    @Override
    public Uni<Map<V, Integer>> topkListWithCount(K key) {
        return _topkListWithCount(key).toUni();
    }

    LettuceCommand<List<TopKListValue>, Map<V, Integer>> _topkListWithCount(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.topKList(marshaller.encode(key), true), this::decodeTopKListValueList);
    }

    @Override
    public Uni<Boolean> topkQuery(K key, V item) {
        return _topkQuery(key, item).toUni();
    }

    LettuceCommand<List<Boolean>, Boolean> _topkQuery(K key, V item) {
        nonNull(key, "key");
        nonNull(item, "item");
        return LettuceCommand.of(() -> async.topKQuery(marshaller.encode(key), marshaller.encode(item)),
                List::getFirst);
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> topkQuery(K key, V... items) {
        return _topkQuery(key, items).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _topkQuery(K key, V... items) {
        nonNull(key, "key");
        doesNotContainNull(items, "items");
        if (items.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`items` must not be empty"));
        }
        return LettuceCommand.of(() -> async.topKQuery(marshaller.encode(key), marshaller.encodeAsArray(items)),
                AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk) {
        return _topkReserve(key, topk).toUni();
    }

    LettuceCommand<String, Void> _topkReserve(K key, int topk) {
        nonNull(key, "key");
        positive(topk, "topk");
        return LettuceCommand.discarding(() -> async.topKReserve(marshaller.encode(key), topk));
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk, int width, int depth, double decay) {
        return _topkReserve(key, topk, width, depth, decay).toUni();
    }

    LettuceCommand<String, Void> _topkReserve(K key, int topk, int width, int depth, double decay) {
        nonNull(key, "key");
        positive(topk, "topk");
        TopKReserveArgs args = TopKReserveArgs.Builder.width(width).depth(depth).decay(decay);
        return LettuceCommand.discarding(() -> async.topKReserve(marshaller.encode(key), topk, args));
    }

    private V decodeValue(String item) {
        return item == null ? null : decodeValue(item.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private IncrementPair<byte[]>[] encodeCouples(Map<V, Integer> couples) {
        IncrementPair<byte[]>[] pairs = new IncrementPair[couples.size()];
        int i = 0;
        for (Map.Entry<V, Integer> entry : couples.entrySet()) {
            nonNull(entry.getKey(), "couples.key");
            nonNull(entry.getValue(), "couples.value");
            pairs[i++] = new IncrementPair<>(marshaller.encode(entry.getKey()), entry.getValue());
        }
        return pairs;
    }

    private Map<V, V> zipDropped(List<V> items, List<String> dropped) {
        if (dropped == null || dropped.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<V, V> result = LinkedHashMap.newLinkedHashMap(items.size());
        for (int i = 0; i < items.size(); i++) {
            result.put(items.get(i), decodeValue(dropped.get(i)));
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<V, Integer> decodeTopKListValueList(List<TopKListValue> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<V, Integer> result = LinkedHashMap.newLinkedHashMap(values.size());
        for (TopKListValue value : values) {
            result.put(decodeValue(value.getName()), value.getCount().intValue());
        }
        return Collections.unmodifiableMap(result);
    }

}
