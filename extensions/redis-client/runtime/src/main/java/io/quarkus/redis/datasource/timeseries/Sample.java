package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.util.Objects;

/**
 * Represents a sample from a time series
 */
public class Sample {

    public final long timestamp;
    public final double value;

    public Sample(long timestamp, double value) {
        this.timestamp = positiveOrZero(timestamp, "timestamp");
        this.value = value;
    }

    public long timestamp() {
        return timestamp;
    }

    public double value() {
        return value;
    }

    @Override
    public String toString() {
        return "Sample{" + "timestamp=" + timestamp + ", value=" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Sample sample = (Sample) o;
        return timestamp == sample.timestamp && Double.compare(sample.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value);
    }
}
