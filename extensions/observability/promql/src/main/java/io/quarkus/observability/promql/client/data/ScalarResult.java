package io.quarkus.observability.promql.client.data;

import java.time.Instant;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

@JsonSerialize(converter = ScalarResult.ConvertToArray.class)
@JsonDeserialize(converter = ScalarResult.ConvertFromArray.class)
public class ScalarResult {

    private final Instant time;
    private final double value;

    public ScalarResult(
            Instant time,
            double value) {
        this.time = time;
        this.value = value;
    }

    public Instant time() {
        return time;
    }

    public double value() {
        return value;
    }

    public static class ConvertToArray implements Converter<ScalarResult, Object[]> {
        @Override
        public Object[] convert(ScalarResult result) {
            double epochSeconds = (double) result.time().getEpochSecond() + (double) result.time().getNano() / 1000_000_000d;
            double value = result.value();
            String stringValue;
            if (Double.isNaN(value)) {
                stringValue = "NaN";
            } else if (Double.isInfinite(value)) {
                if (value < 0) {
                    stringValue = "-Inf";
                } else {
                    stringValue = "Inf";
                }
            } else {
                stringValue = String.valueOf(value);
            }
            return new Object[] { epochSeconds, stringValue };
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(ScalarResult.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(Object[].class);
        }
    }

    public static class ConvertFromArray implements Converter<Object[], ScalarResult> {
        @Override
        public ScalarResult convert(Object[] tuple) {
            if (tuple.length != 2) {
                throw new IllegalArgumentException("Two elements expected in ScalarResult");
            }
            double epochSeconds = ((Number) tuple[0]).doubleValue();
            Instant time = Instant.ofEpochSecond(
                    (long) epochSeconds,
                    ((long) (epochSeconds * 1000_000_000d)) % 1000_000_000L);
            String stringValue = (String) tuple[1];
            double value = fromString(stringValue);
            return new ScalarResult(time, value);
        }

        private double fromString(String stringValue) {
            switch (stringValue) {
                case "NaN":
                    return Double.NaN;
                case "-Inf":
                    return Double.NEGATIVE_INFINITY;
                case "Inf":
                    return Double.POSITIVE_INFINITY;
                default:
                    return Double.parseDouble(stringValue);
            }
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(Object[].class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(ScalarResult.class);
        }
    }
}
