package io.quarkus.redis.datasource.sortedset;

import java.util.Objects;

/**
 * A value associated with its score (double)
 */
public class ScoredValue<V> {

    public static ScoredValue<?> EMPTY = new ScoredValue<>(null, 0.0);

    public static <V> ScoredValue<V> of(V v, double score) {
        return new ScoredValue<>(v, score);
    }

    public final V value;
    public final double score;

    @SuppressWarnings("unchecked")
    public static <T> ScoredValue<T> empty() {
        return (ScoredValue<T>) EMPTY;
    }

    public ScoredValue(V value, double score) {
        this.value = value;
        this.score = score;
    }

    public V value() {
        return value;
    }

    public double score() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScoredValue<?> that = (ScoredValue<?>) o;
        return Double.compare(that.score, score) == 0 && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, score);
    }

    @Override
    public String toString() {
        return "ScoredValue{" + "value=" + value + ", score=" + score + '}';
    }
}
