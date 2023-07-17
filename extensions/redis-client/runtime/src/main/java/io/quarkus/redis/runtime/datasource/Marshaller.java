package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.codecs.Codec;
import io.quarkus.redis.datasource.codecs.Codecs;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class Marshaller {

    public static final TypeReference<String> STRING_TYPE_REFERENCE = new TypeReference<String>() {
        // Empty on purpose
    };

    Map<Type, Codec> codecs = new ConcurrentHashMap<>();

    public Marshaller(Type... hints) {
        addAll(hints);
    }

    public void addAll(Type... hints) {
        doesNotContainNull(hints, "hints");
        for (Type hint : hints) {
            codecs.computeIfAbsent(hint, h -> Codecs.getDefaultCodecFor(hint));
        }
    }

    public void add(Class<?> hint) {
        codecs.computeIfAbsent(hint, h -> Codecs.getDefaultCodecFor(hint));
    }

    public byte[] encode(Object o) {
        if (o instanceof String) {
            return ((String) o).getBytes(StandardCharsets.UTF_8);
        }
        if (o == null) {
            return null;
        }
        Class<?> clazz = o.getClass();
        Codec codec = codec(clazz);
        return codec.encode(o);
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

    Codec codec(Type clazz) {
        Codec codec = codecs.get(clazz);
        if (codec == null) {
            codec = Codecs.getDefaultCodecFor(clazz);
            codecs.put(clazz, codec);
        }
        return codec;
    }

    public final <T> T decode(Type clazz, Response r) {
        if (r == null) {
            return null;
        }
        if (r.type() == ResponseType.SIMPLE) {
            return decode(clazz, r.toString().getBytes());
        }
        return decode(clazz, r.toBytes());
    }

    @SuppressWarnings("unchecked")
    public final <T> T decode(Type clazz, byte[] r) {
        if (r == null) {
            return null;
        }
        Codec codec = codec(clazz);
        return (T) codec.decode(r);
    }

    public <F, V> Map<F, V> decodeAsMap(Response response, Type typeOfField, Type typeOfValue) {
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

    public <F> List<F> decodeAsList(Response response, Type typeOfItem) {
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

    public <F> Set<F> decodeAsSet(Response response, Type typeOfItem) {
        if (response == null) {
            return Collections.emptySet();
        }
        Set<F> set = new HashSet<>();
        for (Response item : response) {
            set.add(decode(typeOfItem, item));
        }
        return set;
    }

    final <F, V> Map<F, V> decodeAsOrderedMap(Response response, Type typeOfValue, F[] fields) {
        Iterator<Response> iterator = response.iterator();
        Map<F, V> map = new LinkedHashMap<>();
        for (F field : fields) {
            Response v = iterator.next();
            map.put(field, decode(typeOfValue, v));
        }
        return map;
    }
}
