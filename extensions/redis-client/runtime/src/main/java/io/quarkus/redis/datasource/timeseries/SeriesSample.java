package io.quarkus.redis.datasource.timeseries;

/**
 * Represents a sample to be added to a specific time series {@code key}
 */
public class SeriesSample<K> extends Sample {

    public final K key;

    public SeriesSample(K key, long timestamp, double value) {
        super(timestamp, value);
        this.key = key;
    }

    public static <K> SeriesSample<K> from(K k, long ts, double val) {
        return new SeriesSample<>(k, ts, val);
    }

    public static <K> SeriesSample<K> from(K k, double val) {
        return new SeriesSample<>(k, Long.MAX_VALUE, val);
    }

    public K key() {
        return key;
    }
}
