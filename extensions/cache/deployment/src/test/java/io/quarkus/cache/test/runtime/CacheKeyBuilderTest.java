package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.runtime.CacheKeyBuilder;

public class CacheKeyBuilderTest {

    @Test
    public void testInvalidKeyElements() {
        assertThrows(IllegalArgumentException.class, () -> {
            CacheKeyBuilder.build(null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CacheKeyBuilder.build(Collections.emptyList());
        });
        assertThrows(NullPointerException.class, () -> {
            CacheKeyBuilder.build(Collections.singletonList(null));
        }, CacheKeyBuilder.NULL_KEYS_NOT_SUPPORTED_MSG);
    }

    @Test
    public void testSimpleKey() {
        String keyElement = "quarkus";

        // A cache key with one element should be the element itself (same object reference).
        Object simpleKey1 = CacheKeyBuilder.build(Collections.singletonList(keyElement));
        assertTrue(keyElement == simpleKey1);

        // Two cache keys built from the same single element should have the same object reference.
        Object simpleKey2 = CacheKeyBuilder.build(Collections.singletonList(keyElement));
        assertTrue(simpleKey1 == simpleKey2);

        // Two cache keys built from different single elements should not be equal.
        Object simpleKey3 = CacheKeyBuilder.build(Collections.singletonList(Boolean.valueOf("true")));
        assertNotEquals(simpleKey2, simpleKey3);
    }

    @Test
    public void testCompositeKey() {
        String keyElement1 = "quarkus";
        long keyElement2 = 123L;
        Object keyElement3 = new Object();

        // Two cache keys built from the same elements and in the same order should be equal.
        Object compositeKey1 = CacheKeyBuilder.build(Arrays.asList(keyElement1, keyElement2, keyElement3));
        Object compositeKey2 = CacheKeyBuilder.build(Arrays.asList(keyElement1, keyElement2, keyElement3));
        assertEquals(compositeKey1, compositeKey2);

        // Two cache keys built from the same elements but not in the same order should not be equal.
        Object compositeKey3 = CacheKeyBuilder.build(Arrays.asList(keyElement2, keyElement1, keyElement3));
        assertNotEquals(compositeKey2, compositeKey3);

        // Two cache keys built from a different number of elements should not be equal.
        Object compositeKey4 = CacheKeyBuilder
                .build(Arrays.asList(keyElement1, keyElement2, keyElement3, keyElement1, keyElement2, keyElement3));
        assertNotEquals(compositeKey2, compositeKey4);

        // Two cache keys built from multiple elements should only be equal if all elements are equal pairwise.
        Object compositeKey5 = CacheKeyBuilder.build(Arrays.asList(new AtomicInteger(456), keyElement2, keyElement3));
        assertNotEquals(compositeKey2, compositeKey5);
        Object compositeKey6 = CacheKeyBuilder.build(Arrays.asList(keyElement1, new Object(), keyElement3));
        assertNotEquals(compositeKey2, compositeKey6);
        Object compositeKey7 = CacheKeyBuilder.build(Arrays.asList(keyElement1, keyElement2, new BigDecimal(10)));
        assertNotEquals(compositeKey2, compositeKey7);
    }
}
