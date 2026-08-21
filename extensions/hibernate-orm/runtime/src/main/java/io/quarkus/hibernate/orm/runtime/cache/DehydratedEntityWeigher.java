package io.quarkus.hibernate.orm.runtime.cache;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;

import com.github.benmanes.caffeine.cache.Weigher;

/**
 * Estimates a <em>relative</em> weight for Hibernate second-level cache entries.
 * <p>
 * These weights are eviction accounting units for Caffeine's {@code maximumWeight},
 * not a measurement of JVM heap bytes. Exact instance sizes depend on the runtime
 * (compressed oops, object headers / Lilliput, alignment, JVM vendor), which this
 * weigher deliberately does not attempt to model.
 * <p>
 * Hibernate 2LC stores dehydrated entity state (typically {@link CacheEntry} with a
 * disassembled state array), not live entity instances. This weigher walks that state
 * and assigns weight primarily from variable-size values such as {@link String} and
 * {@code byte[]} payloads.
 * <h2>Heuristics</h2>
 * <ul>
 * <li>Each entry starts with a small base weight.</li>
 * <li>{@link String} / {@code char[]} contribute roughly {@code length * 2}.</li>
 * <li>{@code byte[]} contribute roughly {@code length}.</li>
 * <li>Common scalars (numbers, booleans, enums, temporals, UUID, …) contribute a
 * flat scalar weight.</li>
 * <li>Unrecognized values contribute a fixed unknown-type weight.</li>
 * </ul>
 * <h2>Known gaps</h2>
 * Estimates can diverge substantially from true heap usage for custom
 * {@link Serializable} graphs, nested structures beyond shallow arrays, and values
 * that fall into the unknown-type bucket. Object header and reference costs are
 * ignored. Applications that need different accounting should provide a custom
 * {@link Weigher} CDI bean annotated with
 * {@code io.quarkus.hibernate.orm.PersistenceUnitExtension}.
 */
public final class DehydratedEntityWeigher implements Weigher<Object, Object> {

    public static final DehydratedEntityWeigher INSTANCE = new DehydratedEntityWeigher();

    private static final int UNKNOWN_WEIGHT = 100;
    private static final int BASE_ENTRY_WEIGHT = 32;
    private static final int SCALAR_WEIGHT = 16;

    private DehydratedEntityWeigher() {
    }

    @Override
    public int weigh(Object key, Object value) {
        long weight = BASE_ENTRY_WEIGHT + estimate(key) + estimateCacheValue(value);
        if (weight > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) weight);
    }

    private static long estimateCacheValue(Object value) {
        if (value instanceof CacheEntry cacheEntry) {
            return estimateArray(cacheEntry.getDisassembledState());
        }
        if (value instanceof CollectionCacheEntry collectionCacheEntry) {
            return estimateArray(collectionCacheEntry.getState());
        }
        return estimate(value);
    }

    private static long estimateArray(Serializable[] state) {
        if (state == null || state.length == 0) {
            return SCALAR_WEIGHT;
        }
        long total = 0;
        for (Serializable element : state) {
            total += estimate(element);
        }
        return total;
    }

    private static long estimate(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof String string) {
            // Relative weight based on character count; good enough for eviction accounting.
            return Math.max(SCALAR_WEIGHT, (long) string.length() * 2);
        }
        if (value instanceof byte[] bytes) {
            return Math.max(SCALAR_WEIGHT, bytes.length);
        }
        if (value instanceof char[] chars) {
            return Math.max(SCALAR_WEIGHT, (long) chars.length * 2);
        }
        if (value instanceof Object[] array) {
            long total = SCALAR_WEIGHT;
            for (Object element : array) {
                total += estimate(element);
            }
            return total;
        }
        if (value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum
                || value instanceof UUID
                || value instanceof Date
                || value instanceof Calendar
                || value instanceof Temporal
                || value instanceof BigDecimal
                || value instanceof BigInteger) {
            return SCALAR_WEIGHT;
        }
        return UNKNOWN_WEIGHT;
    }
}
