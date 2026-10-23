package io.quarkus.redis.lettuce.runtime.internal.countmin;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.isNotEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.probabilistic.MergePair;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveCountMinCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveCountMinCommands<K, V> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveCountMinCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Long> cmsIncrBy(K key, V value, long increment) {
        return _cmsIncrBy(key, value, increment).toUni();
    }

    LettuceCommand<List<Long>, Long> _cmsIncrBy(K key, V value, long increment) {
        nonNull(key, "key");
        nonNull(value, "value");
        IncrementPair<byte[]> pair = new IncrementPair<>(marshaller.encode(value), increment);
        return LettuceCommand.of(() -> async.cmsIncrBy(marshaller.encode(key), pair), List::getFirst);
    }

    @Override
    public Uni<Map<V, Long>> cmsIncrBy(K key, Map<V, Long> couples) {
        return _cmsIncrBy(key, couples).toUni();
    }

    LettuceCommand<List<Long>, Map<V, Long>> _cmsIncrBy(K key, Map<V, Long> couples) {
        nonNull(key, "key");
        nonNull(couples, "couples");
        if (couples.isEmpty()) {
            return LettuceCommand.failing(new IllegalArgumentException("`couples` must not be empty"));
        }
        // CMS.INCRBY replies with one count per pair, in the order the pairs were sent. Snapshot the keys in that
        // same iteration order so the reply can be zipped back into a map even if the caller's map changes later.
        List<V> values = new ArrayList<>(couples.keySet());
        IncrementPair<byte[]>[] pairs = encodeCouples(couples);
        return LettuceCommand.of(() -> async.cmsIncrBy(marshaller.encode(key), pairs),
                counts -> zipCounts(values, counts));
    }

    @Override
    public Uni<Void> cmsInitByDim(K key, long width, long depth) {
        return _cmsInitByDim(key, width, depth).toUni();
    }

    LettuceCommand<String, Void> _cmsInitByDim(K key, long width, long depth) {
        nonNull(key, "key");
        return LettuceCommand.discarding(() -> async.cmsInitByDim(marshaller.encode(key), width, depth));
    }

    @Override
    public Uni<Void> cmsInitByProb(K key, double error, double probability) {
        return _cmsInitByProb(key, error, probability).toUni();
    }

    LettuceCommand<String, Void> _cmsInitByProb(K key, double error, double probability) {
        nonNull(key, "key");
        return LettuceCommand.discarding(() -> async.cmsInitByProb(marshaller.encode(key), error, probability));
    }

    @Override
    public Uni<Long> cmsQuery(K key, V item) {
        return _cmsQuery(key, item).toUni();
    }

    LettuceCommand<List<Long>, Long> _cmsQuery(K key, V item) {
        nonNull(key, "key");
        nonNull(item, "item");
        return LettuceCommand.of(() -> async.cmsQuery(marshaller.encode(key), marshaller.encode(item)),
                List::getFirst);
    }

    @SafeVarargs
    @Override
    public final Uni<List<Long>> cmsQuery(K key, V... items) {
        return _cmsQuery(key, items).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Long>, List<Long>> _cmsQuery(K key, V... items) {
        nonNull(key, "key");
        doesNotContainNull(items, "items");
        if (items.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`items` must not be empty"));
        }
        return LettuceCommand.of(() -> async.cmsQuery(marshaller.encode(key), marshaller.encodeAsArray(items)),
                AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<Void> cmsMerge(K dest, List<K> src, List<Integer> weight) {
        return _cmsMerge(dest, src, weight).toUni();
    }

    LettuceCommand<String, Void> _cmsMerge(K dest, List<K> src, List<Integer> weight) {
        nonNull(dest, "dest");
        doesNotContainNull(src, "src");
        isNotEmpty(src, "src");
        if (weight == null || weight.isEmpty()) {
            byte[][] sources = src.stream().map(marshaller::encode).toArray(byte[][]::new);
            return LettuceCommand.discarding(() -> async.cmsMerge(marshaller.encode(dest), sources));
        }
        doesNotContainNull(weight, "weight");
        if (weight.size() != src.size()) {
            return LettuceCommand.failing(new IllegalArgumentException(
                    "`weight` must contain exactly one weight per source sketch (" + src.size() + " expected, "
                            + weight.size() + " given)"));
        }
        MergePair<byte[]>[] pairs = encodeMergePairs(src, weight);
        return LettuceCommand.discarding(() -> async.cmsMerge(marshaller.encode(dest), pairs));
    }

    @SuppressWarnings("unchecked")
    private IncrementPair<byte[]>[] encodeCouples(Map<V, Long> couples) {
        IncrementPair<byte[]>[] pairs = new IncrementPair[couples.size()];
        int i = 0;
        for (Map.Entry<V, Long> entry : couples.entrySet()) {
            nonNull(entry.getKey(), "couples.key");
            nonNull(entry.getValue(), "couples.value");
            pairs[i++] = new IncrementPair<>(marshaller.encode(entry.getKey()), entry.getValue());
        }
        return pairs;
    }

    @SuppressWarnings("unchecked")
    private MergePair<byte[]>[] encodeMergePairs(List<K> src, List<Integer> weight) {
        MergePair<byte[]>[] pairs = new MergePair[src.size()];
        for (int i = 0; i < src.size(); i++) {
            pairs[i] = new MergePair<>(marshaller.encode(src.get(i)), weight.get(i));
        }
        return pairs;
    }

    private static <V> Map<V, Long> zipCounts(List<V> values, List<Long> counts) {
        if (counts == null || counts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<V, Long> result = LinkedHashMap.newLinkedHashMap(values.size());
        for (int i = 0; i < values.size(); i++) {
            result.put(values.get(i), counts.get(i));
        }
        return Collections.unmodifiableMap(result);
    }

}
