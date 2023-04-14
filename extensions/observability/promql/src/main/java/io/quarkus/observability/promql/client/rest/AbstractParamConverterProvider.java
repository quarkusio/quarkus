package io.quarkus.observability.promql.client.rest;

import java.util.Objects;
import java.util.function.Function;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

public abstract class AbstractParamConverterProvider implements ParamConverterProvider {
    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    protected static <T> ParamConverter<T> cast(Class<T> rawType, ParamConverter<?> paramConverter) {
        return (ParamConverter) paramConverter;
    }

    public static final class PC<T> implements ParamConverter<T> {
        private final Function<String, T> fromStringFn;
        private final Function<T, String> toStringFn;

        public PC(Function<String, T> fromStringFn, Function<T, String> toStringFn) {
            this.fromStringFn = Objects.requireNonNull(fromStringFn);
            this.toStringFn = Objects.requireNonNull(toStringFn);
        }

        @Override
        public T fromString(String value) {
            return value == null ? null : fromStringFn.apply(value);
        }

        @Override
        public String toString(T value) {
            return value == null ? null : toStringFn.apply(value);
        }
    }
}
