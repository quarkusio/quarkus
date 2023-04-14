package io.quarkus.observability.promql.client.data;

import java.time.Instant;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

@JsonSerialize(converter = StringResult.ConvertToArray.class)
@JsonDeserialize(converter = StringResult.ConvertFromArray.class)
public class StringResult {
    private final Instant time;
    private final String value;

    public StringResult(Instant time, String value) {
        this.time = time;
        this.value = value;
    }

    public Instant time() {
        return time;
    }

    public String value() {
        return value;
    }

    public static class ConvertToArray implements Converter<StringResult, Object[]> {
        @Override
        public Object[] convert(StringResult result) {
            double epochSeconds = (double) result.time().getEpochSecond() + (double) result.time().getNano() / 1000_000_000d;
            String stringValue = result.value();
            return new Object[] { epochSeconds, stringValue };
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(StringResult.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(Object[].class);
        }
    }

    public static class ConvertFromArray implements Converter<Object[], StringResult> {
        @Override
        public StringResult convert(Object[] tuple) {
            if (tuple.length != 2) {
                throw new IllegalArgumentException("Two elements expected in StringResult");
            }
            double epochSeconds = ((Number) tuple[0]).doubleValue();
            Instant time = Instant.ofEpochSecond(
                    (long) epochSeconds,
                    ((long) (epochSeconds * 1000_000_000d)) % 1000_000_000L);
            String stringValue = (String) tuple[1];
            return new StringResult(time, stringValue);
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(Object[].class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(StringResult.class);
        }
    }
}
