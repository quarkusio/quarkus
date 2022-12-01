package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.redis.datasource.codecs.Codec;
import io.quarkus.redis.datasource.codecs.Codecs;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class Marshaller {

    private static final Map<Class<?>, Codec<?>> DEFAULT_CODECS;

    static {
        DEFAULT_CODECS = new HashMap<>();
        DEFAULT_CODECS.put(String.class, Codecs.StringCodec.INSTANCE);
        DEFAULT_CODECS.put(Integer.class, Codecs.IntegerCodec.INSTANCE);
        DEFAULT_CODECS.put(Double.class, Codecs.DoubleCodec.INSTANCE);
    }

    Map<Class<?>, Codec<?>> codecs = new HashMap<>();

    public Marshaller(Class<?>... hints) {
        doesNotContainNull(hints, "hints");

        for (Class<?> hint : hints) {
            codecs.put(hint, Codecs.getDefaultCodecFor(hint));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public byte[] encode(Object o) {
        if (o instanceof String) {
            return ((String) o).getBytes(StandardCharsets.UTF_8);
        }
        if (o == null) {
            return null;
        }
        Class<?> clazz = o.getClass();
        Codec codec = codec(clazz);
        if (codec != null) {
            return codec.encode(o);
        } else {
            throw new IllegalArgumentException("Unable to encode object of type " + clazz);
        }
    }

    @SafeVarargs
    public final <T> List<byte[]> encode(T... objects) {
        nonNull(objects, "objects");
        List<byte[]> result = new ArrayList<>();
        for (T o : objects) {
            byte[] r = encode(o);
            result.add(r);
        }
        return result;
    }

    Codec<?> codec(Class<?> clazz) {
        Codec<?> codec = codecs.get(clazz);
        if (codec == null) {
            codec = DEFAULT_CODECS.get(clazz);
        }
        return codec;
    }

    final <T> T decode(Class<T> clazz, Response r) {
        if (r == null) {
            return null;
        }
        if (r.type() == ResponseType.SIMPLE) {
            return decode(clazz, r.toString().getBytes());
        }
        return decode(clazz, r.toBytes());
    }

    @SuppressWarnings("unchecked")
    public final <T> T decode(Class<T> clazz, byte[] r) {
        if (r == null) {
            return null;
        }
        Codec<?> codec = codec(clazz);
        return (T) codec.decode(r);
    }

    public <F, V> Map<F, V> decodeAsMap(Response response, Class<F> typeOfField, Class<V> typeOfValue) {
        if (response == null || response.size() == 0) {
            return Collections.emptyMap();
        }
        Map<F, V> map = new LinkedHashMap<>();
        if (response.iterator().next().type() == ResponseType.BULK) {
            // Redis 5
            F current = null; // Just in case it's Redis 5.
            for (Response member : response) {
                if (current == null) {
                    current = decode(typeOfField, member.toString().getBytes(StandardCharsets.UTF_8));
                } else {
                    V val = decode(typeOfValue, member);
                    map.put(current, val);
                    current = null;
                }
            }
        } else {
            // MULTI - Redis 6+
            for (Response member : response) {
                for (String key : member.getKeys()) {
                    F field = decode(typeOfField, key.getBytes(StandardCharsets.UTF_8));
                    V val = decode(typeOfValue, response.get(key));
                    map.put(field, val);
                }
            }
        }
        return map;
    }

    public <F> List<F> decodeAsList(Response response, Class<F> typeOfItem) {
        if (response == null) {
            return Collections.emptyList();
        }
        List<F> list = new ArrayList<>();
        for (Response item : response) {
            list.add(decode(typeOfItem, item));
        }
        return list;
    }

    public <T> List<T> decodeAsList(Response response, Function<Response, T> mapper) {
        if (response == null) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>();
        for (Response item : response) {
            if (item == null) {
                list.add(null);
            } else {
                list.add(mapper.apply(item));
            }
        }
        return list;
    }

    public <F> Set<F> decodeAsSet(Response response, Class<F> typeOfItem) {
        if (response == null) {
            return Collections.emptySet();
        }
        Set<F> set = new HashSet<>();
        for (Response item : response) {
            set.add(decode(typeOfItem, item));
        }
        return set;
    }

    final <F, V> Map<F, V> decodeAsOrderedMap(Response response, Class<V> typeOfValue, F[] fields) {
        Iterator<Response> iterator = response.iterator();
        Map<F, V> map = new LinkedHashMap<>();
        for (F field : fields) {
            Response v = iterator.next();
            map.put(field, decode(typeOfValue, v));
        }
        return map;
    }
}
