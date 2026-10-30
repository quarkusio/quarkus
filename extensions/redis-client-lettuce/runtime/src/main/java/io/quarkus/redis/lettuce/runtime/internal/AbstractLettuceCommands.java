package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.lettuce.core.KeyValue;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

/**
 * Base class for Lettuce-backed reactive command group implementations.
 * <p>
 * Holds the async command handle and helper methods. The handle is the {@link RedisClusterAsyncCommands}
 * surface shared by the standalone and the cluster connections (see {@link LettuceConnection}), so a
 * command group works on both topologies. Subclasses whose command group has blocking
 * commands pass a {@code pool}, used by {@link #blocking(Function)} to route
 * those commands off the shared connection; other groups pass {@code null}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public abstract class AbstractLettuceCommands<K, V> {

    protected final RedisClusterAsyncCommands<byte[], byte[]> async;
    protected final Type keyType;
    protected final Type valueType;
    protected final Marshaller marshaller;
    protected final LettuceConnectionPool pool;

    protected AbstractLettuceCommands(LettuceConnection connection, Type keyType, Type valueType,
            Marshaller marshaller) {
        this(connection, keyType, valueType, marshaller, null);
    }

    protected AbstractLettuceCommands(LettuceConnection connection, Type keyType, Type valueType,
            Marshaller marshaller, LettuceConnectionPool pool) {
        nonNull(connection, "connection");
        nonNull(keyType, "keyType");
        nonNull(valueType, "valueType");
        nonNull(marshaller, "marshaller");
        this.async = connection.async();
        this.keyType = keyType;
        this.valueType = valueType;
        this.marshaller = marshaller;
        this.pool = pool;
    }

    // ---------------------------------------------------------------------------------------------
    // Command execution
    // ---------------------------------------------------------------------------------------------

    protected <T, R> Uni<R> blocking(Function<RedisClusterAsyncCommands<byte[], byte[]>, LettuceCommand<T, R>> builder) {
        LettuceCommand<T, R> validated = builder.apply(async);
        if (pool == null) {
            return validated.toUni();
        }
        return pool.withPooled(conn -> builder.apply(conn.async()).toUni());
    }

    // ---------------------------------------------------------------------------------------------
    // Reply normalisation (type-independent)
    // ---------------------------------------------------------------------------------------------

    protected static boolean isOk(String response) {
        return "OK".equals(response);
    }

    protected static Boolean toBoolean(Long added) {
        return added != null && added == 1L;
    }

    protected static Long orZero(Long value) {
        if (value == null) {
            return 0L;
        }
        return value;
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

    protected static <T> List<T> decodeList(List<T> reply) {
        if (reply == null || reply.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(reply);
    }

    protected static <A, B> List<B> decodeList(List<A> reply, Function<A, B> mapper) {
        if (reply == null || reply.isEmpty()) {
            return Collections.emptyList();
        }
        return reply.stream().map(mapper).toList();
    }

    protected static <T> Set<T> decodeSet(List<T> reply) {
        if (reply == null || reply.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(reply));
    }

    protected static <A, B> Set<B> decodeSet(Set<A> reply, Function<A, B> mapper) {
        if (reply == null || reply.isEmpty()) {
            return Collections.emptySet();
        }
        Set<B> decoded = reply.stream().map(mapper).collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(decoded);
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

    protected Set<V> decodeSetOfValues(Set<byte[]> list) {
        return decodeSet(list, this::decodeValue);
    }

    protected <F> Map<F, V> decodeMap(Type fieldType, Map<byte[], byte[]> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<F, V> decoded = LinkedHashMap.newLinkedHashMap(map.size());
        for (Map.Entry<byte[], byte[]> e : map.entrySet()) {
            decoded.put(marshaller.decode(fieldType, e.getKey()), decodeValue(e.getValue()));
        }
        return Collections.unmodifiableMap(decoded);
    }

    protected <T> Map<T, V> decodeOrderedMap(T[] keys, List<KeyValue<byte[], byte[]>> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<T, V> map = LinkedHashMap.newLinkedHashMap(keys.length);
        for (int i = 0; i < keys.length; i++) {
            KeyValue<byte[], byte[]> kv = results.get(i);
            map.put(keys[i], kv.hasValue() ? decodeValue(kv.getValue()) : null);
        }
        return Collections.unmodifiableMap(map);
    }

}
