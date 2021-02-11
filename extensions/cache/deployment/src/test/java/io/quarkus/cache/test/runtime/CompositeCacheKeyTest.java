package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.runtime.CompositeCacheKey;

public class CompositeCacheKeyTest {

    private static final String KEY_ELEMENT_1 = "test";
    private static final long KEY_ELEMENT_2 = 123L;
    private static final Object KEY_ELEMENT_3 = new Object();

    @Test
    public void testEquality() {

        // Two composite keys built from the same elements and in the same order should be equal.
        CompositeCacheKey key1 = new CompositeCacheKey(KEY_ELEMENT_1, KEY_ELEMENT_2, KEY_ELEMENT_3);
        CompositeCacheKey key2 = new CompositeCacheKey(KEY_ELEMENT_1, KEY_ELEMENT_2, KEY_ELEMENT_3);
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());

        // If the same elements are used but their order is changed, the keys should not be equal.
        CompositeCacheKey key3 = new CompositeCacheKey(KEY_ELEMENT_2, KEY_ELEMENT_1, KEY_ELEMENT_3);
        assertNotEquals(key2, key3);

        // If the numbers of elements are different, the keys should not be equal.
        CompositeCacheKey key4 = new CompositeCacheKey(KEY_ELEMENT_1, KEY_ELEMENT_2, KEY_ELEMENT_3, new Object());
        assertNotEquals(key2, key4);

        // Two composite keys built from multiple elements should only be equal if all elements are equal pairwise.
        CompositeCacheKey key5 = new CompositeCacheKey(Boolean.TRUE, KEY_ELEMENT_2, KEY_ELEMENT_3);
        assertNotEquals(key2, key5);
        CompositeCacheKey key6 = new CompositeCacheKey(KEY_ELEMENT_1, new Object(), KEY_ELEMENT_3);
        assertNotEquals(key2, key6);
        CompositeCacheKey key7 = new CompositeCacheKey(KEY_ELEMENT_1, KEY_ELEMENT_2, BigDecimal.TEN);
        assertNotEquals(key2, key7);
    }

    @Test
    public void testNoElementsKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CompositeCacheKey();
        });
    }
}
