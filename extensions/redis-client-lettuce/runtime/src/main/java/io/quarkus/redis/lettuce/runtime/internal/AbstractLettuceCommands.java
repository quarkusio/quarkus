package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

/**
 * Base class for Lettuce-backed reactive command group implementations.
 * <p>
 * Holds the owning data source, the source of the connection and helper methods. A command is a function of the
 * async command handle of the connection it is issued on (see {@link #command(Function, Function)}): the connection
 * of the source, which a shared connection only opens for its first command, or the one a transaction or a blocking
 * command runs on. The handle is the {@link RedisClusterAsyncCommands}
 * surface shared by the standalone and the cluster connections (see {@link LettuceConnection}), so a
 * command group works on both topologies. Subclasses whose command group has blocking
 * commands pass a {@code pool}, used by {@link #blocking(LettuceCommand)} to route
 * those commands off the shared connection; other groups pass {@code null}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public abstract class AbstractLettuceCommands<K, V> implements ReactiveRedisCommands {

    protected final ReactiveRedisDataSource dataSource;
    protected final LettuceConnectionSource source;
    protected final Type keyType;
    protected final Type valueType;
    protected final Marshaller marshaller;
    protected final LettuceConnectionPool pool;

    protected AbstractLettuceCommands(ReactiveRedisDataSource dataSource, LettuceConnectionSource source, Type keyType,
            Type valueType, Marshaller marshaller) {
        this(dataSource, source, keyType, valueType, marshaller, null);
    }

    protected AbstractLettuceCommands(ReactiveRedisDataSource dataSource, LettuceConnectionSource source, Type keyType,
            Type valueType, Marshaller marshaller, LettuceConnectionPool pool) {
        nonNull(dataSource, "dataSource");
        nonNull(source, "source");
        nonNull(keyType, "keyType");
        nonNull(valueType, "valueType");
        nonNull(marshaller, "marshaller");
        this.dataSource = dataSource;
        this.source = source;
        this.keyType = keyType;
        this.valueType = valueType;
        this.marshaller = marshaller;
        this.pool = pool;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    // ---------------------------------------------------------------------------------------------
    // Command execution
    // ---------------------------------------------------------------------------------------------

    /**
     * A command of this group: {@code call} on the async command handle of the connection it is issued on, its
     * result converted by {@code mapper}.
     */
    protected <T, R> LettuceCommand<T, R> command(Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call,
            Function<? super T, ? extends R> mapper) {
        return LettuceCommand.of(source, call, mapper);
    }

    protected <T> LettuceCommand<T, T> command(Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call) {
        return LettuceCommand.of(source, call);
    }

    protected <T> LettuceCommand<T, Void> discarding(
            Function<RedisClusterAsyncCommands<byte[], byte[]>, RedisFuture<T>> call) {
        return LettuceCommand.discarding(source, call);
    }

    protected static <T, R> LettuceCommand<T, R> failing(RuntimeException failure) {
        return LettuceCommand.failing(failure);
    }

    /**
     * Runs a blocking command on a connection borrowed from the pool for its duration, or on the connection of
     * this group when it has no pool: the pinned connection of a scope.
     */
    protected <T, R> Uni<R> blocking(LettuceCommand<T, R> command) {
        if (pool == null) {
            return command.toUni();
        }
        return pool.withPooled(pooled -> command.on(pooled).toUni());
    }

    // ---------------------------------------------------------------------------------------------
    // Reply normalisation
    // ---------------------------------------------------------------------------------------------

    protected static boolean isOk(String response) {
        return "OK".equals(response);
    }

    protected static Boolean toBoolean(Long added) {
        return added != null && added == 1L;
    }

    protected static Long orZero(Long value) {
        return value != null ? value : 0L;
    }

    protected static Integer toInteger(Long value) {
        return value == null ? null : value.intValue();
    }

    protected static List<Boolean> nullsToFalse(List<Boolean> reply) {
        return decodeList(reply, bool -> bool != null && bool);
    }

    protected static List<Integer> toInteger(List<Long> values) {
        return decodeList(values, AbstractLettuceCommands::toInteger);
    }

    // ---------------------------------------------------------------------------------------------
    // Argument encoding
    // ---------------------------------------------------------------------------------------------

    protected static boolean isWholeSeconds(Duration duration) {
        return duration.getNano() == 0;
    }

    protected static double toFractionalSeconds(Duration duration) {
        return duration.toMillis() / 1_000.0d;
    }

    protected <A, B> Map<byte[], byte[]> encodeMap(Map<A, B> map) {
        Map<byte[], byte[]> encoded = LinkedHashMap.newLinkedHashMap(map.size());
        for (Map.Entry<A, B> e : map.entrySet()) {
            byte[] value = marshaller.encode(e.getValue());
            encoded.put(marshaller.encode(nonNull(e.getKey(), "map key")), nonNull(value, "value"));
        }
        return encoded;
    }

    protected <A, B> Map<byte[], byte[]> encodeMapWithNullableValues(Map<A, B> map) {
        Map<byte[], byte[]> encoded = LinkedHashMap.newLinkedHashMap(map.size());
        for (Map.Entry<A, B> e : map.entrySet()) {
            byte[] value = marshaller.encode(e.getValue());
            encoded.put(marshaller.encode(nonNull(e.getKey(), "map key")),
                    value != null ? value : "null".getBytes(StandardCharsets.UTF_8));
        }
        return encoded;
    }

    // ---------------------------------------------------------------------------------------------
    // Reply decoding
    // ---------------------------------------------------------------------------------------------

    protected static <T> List<T> decodeList(Collection<T> reply) {
        return decodeList(reply, Function.identity());
    }

    protected static <A, B> List<B> decodeList(Collection<A> reply, Function<A, B> mapper) {
        if (reply == null || reply.isEmpty()) {
            return Collections.emptyList();
        }
        return reply.stream().map(mapper).toList();
    }

    protected static <T> Set<T> decodeSet(Collection<T> reply) {
        return decodeSet(reply, Function.identity());
    }

    protected static <A, B> Set<B> decodeSet(Collection<A> reply, Function<A, B> mapper) {
        if (reply == null || reply.isEmpty()) {
            return Collections.emptySet();
        }
        Set<B> decoded = reply.stream().map(mapper).collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(decoded);
    }

    protected static <A, K2, V2> Map<K2, V2> decodeMap(Collection<A> reply, Function<A, K2> key,
            Function<A, V2> value) {
        if (reply == null || reply.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<K2, V2> decoded = LinkedHashMap.newLinkedHashMap(reply.size());
        for (A entry : reply) {
            decoded.put(key.apply(entry), value.apply(entry));
        }
        return Collections.unmodifiableMap(decoded);
    }

    protected static <A, B, V2> Map<A, V2> zip(List<A> keys, List<B> values, Function<B, V2> mapper) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<A, V2> zipped = LinkedHashMap.newLinkedHashMap(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            zipped.put(keys.get(i), mapper.apply(values.get(i)));
        }
        return Collections.unmodifiableMap(zipped);
    }

    protected K decodeKey(byte[] key) {
        return marshaller.decode(keyType, key);
    }

    protected V decodeValue(byte[] bytes) {
        return marshaller.decode(valueType, bytes);
    }

    protected String decodeString(byte[] bytes) {
        return marshaller.decode(String.class, bytes);
    }

    protected List<K> decodeListOfKeys(List<byte[]> list) {
        return decodeList(list, this::decodeKey);
    }

    protected List<V> decodeListOfValues(List<byte[]> list) {
        return decodeList(list, this::decodeValue);
    }

    protected Set<K> decodeSetOfKeys(Collection<byte[]> list) {
        return decodeSet(list, this::decodeKey);
    }

    protected Set<V> decodeSetOfValues(Collection<byte[]> list) {
        return decodeSet(list, this::decodeValue);
    }

    protected <F> Map<F, V> decodeMap(Type fieldType, Map<byte[], byte[]> map) {
        return decodeMap(map == null ? null : map.entrySet(),
                e -> marshaller.decode(fieldType, e.getKey()), e -> decodeValue(e.getValue()));
    }

    protected <T> Map<T, V> decodeOrderedMap(T[] keys, List<KeyValue<byte[], byte[]>> results) {
        return zip(Arrays.asList(keys), results, kv -> kv.hasValue() ? decodeValue(kv.getValue()) : null);
    }

}
