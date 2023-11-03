package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class WildcardTypeImplTest {

    @Test
    public void testEqualsAndHashCode() {
        assertEquals(WildcardTypeImpl.defaultInstance(), WildcardTypeImpl.withUpperBound(Object.class));
        assertEquals(WildcardTypeImpl.withLowerBound(String.class), WildcardTypeImpl.withLowerBound(String.class));
        assertNotEquals(WildcardTypeImpl.withLowerBound(String.class), WildcardTypeImpl.withLowerBound(Integer.class));
        assertEquals(WildcardTypeImpl.defaultInstance().hashCode(), WildcardTypeImpl.withUpperBound(Object.class).hashCode());
        assertEquals(WildcardTypeImpl.withLowerBound(String.class).hashCode(),
                WildcardTypeImpl.withLowerBound(String.class).hashCode());
    }

}
