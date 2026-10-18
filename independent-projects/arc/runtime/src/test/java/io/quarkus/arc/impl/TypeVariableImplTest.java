package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class TypeVariableImplTest {

    @Test
    public void testEqualsAndHashCode() {
        assertEquals(new TypeVariableImpl<>("T"), new TypeVariableImpl<>("T"));
        assertNotEquals(new TypeVariableImpl<>("T"), new TypeVariableImpl<>("T", String.class));
        assertEquals(new TypeVariableImpl<>("T").hashCode(), new TypeVariableImpl<>("T").hashCode());
    }

}
