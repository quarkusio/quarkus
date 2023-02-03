package io.quarkus.jaxrs.client.reactive.runtime;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

public abstract class RestClientBase implements Closeable {
    private static final ParamConverter<Byte> BYTE_CONVERTER = new ParamConverter<Byte>() {
        @Override
        public Byte fromString(String value) {
            return value == null ? null : Byte.valueOf(value);
        }

        @Override
        public String toString(Byte value) {
            return value == null ? null : value.toString();
        }
    };
    private static final ParamConverter<Short> SHORT_CONVERTER = new ParamConverter<Short>() {
        @Override
        public Short fromString(String value) {
            return value == null ? null : Short.valueOf(value);
        }

        @Override
        public String toString(Short value) {
            return value == null ? null : value.toString();
        }
    };
    private static final ParamConverter<Integer> INTEGER_CONVERTER = new ParamConverter<Integer>() {
        @Override
        public Integer fromString(String value) {
            return value == null ? null : Integer.valueOf(value);
        }

        @Override
        public String toString(Integer value) {
            return value == null ? null : value.toString();
        }
    };
    private static final ParamConverter<Long> LONG_CONVERTER = new ParamConverter<Long>() {
        @Override
        public Long fromString(String value) {
            return value == null ? null : Long.valueOf(value);
        }

        @Override
        public String toString(Long value) {
            return value == null ? null : value.toString();
        }
    };
    private static final ParamConverter<Float> FLOAT_CONVERTER = new ParamConverter<Float>() {
        @Override
        public Float fromString(String value) {
            return value == null ? null : Float.valueOf(value);
        }

        @Override
        public String toString(Float value) {
            return value == null ? null : value.toString();
        }
    };
    private static final ParamConverter<Double> DOUBLE_CONVERTER = new ParamConverter<Double>() {
        @Override
        public Double fromString(String value) {
            return value == null ? null : Double.valueOf(value);
        }

        @Override
        public String toString(Double value) {
            return value == null ? null : value.toString();
        }
    };
    private static final ParamConverter<Character> CHARACTER_CONVERTER = new ParamConverter<Character>() {
        @Override
        public Character fromString(String value) {
            // this will throw if not enough chars, but that's an error anyway
            return value == null ? null : value.charAt(0);
        }

        @Override
        public String toString(Character value) {
            return value == null ? null : value.toString();
        }
    };
    private static final ParamConverter<Boolean> BOOLEAN_CONVERTER = new ParamConverter<Boolean>() {
        @Override
        public Boolean fromString(String value) {
            return value == null ? null : Boolean.valueOf(value);
        }

        @Override
        public String toString(Boolean value) {
            return value == null ? null : value.toString();
        }
    };
    private final List<ParamConverterProvider> paramConverterProviders;
    private final Map<Class<?>, ParamConverterProvider> providerForClass = new ConcurrentHashMap<>();

    public RestClientBase(List<ParamConverterProvider> providers) {
        this.paramConverterProviders = providers;
    }

    @SuppressWarnings("unused") // used by generated code
    public <T> Object[] convertParamArray(T[] value, Class<T> type, Supplier<Type[]> genericType,
            Supplier<Annotation[][]> methodAnnotations, int paramIndex) {
        ParamConverter<T> converter = getConverter(type, genericType, methodAnnotations, paramIndex);

        if (converter == null) {
            return value;
        } else {
            Object[] result = new Object[value.length];

            for (int i = 0; i < value.length; i++) {
                result[i] = converter.toString(value[i]);
            }
            return result;
        }
    }

    @SuppressWarnings("unused") // used by generated code
    public <T> Object convertParam(T value, Class<T> type, Supplier<Type[]> genericType,
            Supplier<Annotation[][]> methodAnnotations,
            int paramIndex) {
        ParamConverter<T> converter = getConverter(type, genericType, methodAnnotations, paramIndex);
        if (converter != null) {
            return converter.toString(value);
        } else {
            // FIXME: cheating, we should generate a converter for this enum
            if (value instanceof Enum) {
                return ((Enum) value).name();
            }
            return value;
        }
    }

    private <T> ParamConverter<T> getConverter(Class<T> type, Supplier<Type[]> genericType,
            Supplier<Annotation[][]> methodAnnotations,
            int paramIndex) {
        ParamConverterProvider converterProvider = providerForClass.get(type);

        if (converterProvider == null) {
            for (ParamConverterProvider provider : paramConverterProviders) {
                ParamConverter<T> converter = provider.getConverter(type, genericType.get()[paramIndex],
                        methodAnnotations.get()[paramIndex]);
                if (converter != null) {
                    providerForClass.put(type, provider);
                    return converter;
                }
            }
            // FIXME: this should go in favour of generating them, so we can generate them only if used for dead-code elimination
            ParamConverter<T> converter = DEFAULT_PROVIDER.getConverter(type, genericType.get()[paramIndex],
                    methodAnnotations.get()[paramIndex]);
            if (converter != null) {
                providerForClass.put(type, DEFAULT_PROVIDER);
                return converter;
            }
            providerForClass.put(type, NO_PROVIDER);
        } else if (converterProvider != NO_PROVIDER) {
            return converterProvider.getConverter(type, genericType.get()[paramIndex], methodAnnotations.get()[paramIndex]);
        }
        return null;
    }

    private static final ParamConverterProvider DEFAULT_PROVIDER = new ParamConverterProvider() {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType == byte.class || rawType == Byte.class) {
                return (ParamConverter<T>) BYTE_CONVERTER;
            }
            if (rawType == short.class || rawType == Short.class) {
                return (ParamConverter<T>) SHORT_CONVERTER;
            }
            if (rawType == int.class || rawType == Integer.class) {
                return (ParamConverter<T>) INTEGER_CONVERTER;
            }
            if (rawType == long.class || rawType == Long.class) {
                return (ParamConverter<T>) LONG_CONVERTER;
            }
            if (rawType == float.class || rawType == Float.class) {
                return (ParamConverter<T>) FLOAT_CONVERTER;
            }
            if (rawType == double.class || rawType == Double.class) {
                return (ParamConverter<T>) DOUBLE_CONVERTER;
            }
            if (rawType == char.class || rawType == Character.class) {
                return (ParamConverter<T>) CHARACTER_CONVERTER;
            }
            if (rawType == boolean.class || rawType == Boolean.class) {
                return (ParamConverter<T>) BOOLEAN_CONVERTER;
            }
            return null;
        }

    };

    private static final ParamConverterProvider NO_PROVIDER = new ParamConverterProvider() {
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            return null;
        }
    };
}
