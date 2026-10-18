package io.quarkus.funqy.runtime.query;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.arc.impl.Reflections;

/**
 * Turn URI parameter map into an object
 *
 */
public class QueryObjectMapper {
    Function<String, Object> extractor(Type type) {
        return extractor(Reflections.getRawType(type));
    }

    Function<String, Object> extractor(Class clz) {
        if (String.class.equals(clz)) {
            return (strVal) -> {
                return strVal;
            };
        }
        if (clz.equals(long.class) || clz.equals(Long.class)) {
            return (strVal) -> {
                return Long.valueOf(strVal);
            };
        }
        if (clz.equals(int.class) || clz.equals(Integer.class)) {
            return (strVal) -> {
                return Integer.valueOf(strVal);
            };
        }
        if (clz.equals(short.class) || clz.equals(Short.class)) {
            return (strVal) -> {
                return Short.valueOf(strVal);
            };
        }
        if (clz.equals(float.class) || clz.equals(Float.class)) {
            return (strVal) -> {
                return Float.valueOf(strVal);
            };
        }
        if (clz.equals(double.class) || clz.equals(Double.class)) {
            return (strVal) -> {
                return Double.valueOf(strVal);
            };
        }
        if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
            return (strVal) -> {
                return Boolean.valueOf(strVal);
            };
        }
        if (clz.equals(byte.class) || clz.equals(Byte.class)) {
            return (strVal) -> {
                return Byte.valueOf(strVal);
            };
        }
        if (clz.equals(OffsetDateTime.class)) {
            return (strVal) -> {
                return OffsetDateTime.parse(strVal);
            };
        }
        return null;
    }

    Map<Class, QueryObjectReader> readers = new HashMap<>();

    public <T> QueryReader<T> readerFor(Class<T> clz) {
        return readerFor(clz, null);
    }

    public QueryReader readerFor(Type type) {
        Class<?> clazz = null;
        if (type instanceof Class) {
            clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            clazz = (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            throw new RuntimeException("Cannot get QueryReader for: " + type);
        }
        return readerFor(clazz, type);
    }

    public <T> QueryReader<T> readerFor(Class<T> clz, Type genericType) {
        if (clz.equals(Map.class)) {
            return (QueryReader<T>) new QueryMapReader(genericType, this);
        }
        QueryObjectReader reader = readers.get(clz);
        if (reader != null)
            return (QueryReader<T>) reader;
        reader = new QueryObjectReader(clz, this);
        readers.put(clz, reader);
        return (QueryReader<T>) reader;
    }

    QueryPropertySetter setterFor(Class clz, Type genericType) {
        if (clz.equals(List.class)) {
            return new QueryListReader(genericType, this);
        }
        if (clz.equals(Set.class)) {
            return new QuerySetReader(genericType, this);
        }
        return (QueryPropertySetter) readerFor(clz, genericType);
    }

}
