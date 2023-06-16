package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import jakarta.enterprise.util.TypeLiteral;

import org.junit.jupiter.api.Test;

public class ParameterizedTypeImplTest {

    @SuppressWarnings("serial")
    @Test
    public void testEqualsAndHashCode() {
        // List<?>
        ParameterizedTypeImpl parameterizedType1 = new ParameterizedTypeImpl(List.class, WildcardTypeImpl.defaultInstance());
        TypeLiteral<List<?>> literal1 = new TypeLiteral<List<?>>() {
        };
        assertEquals(parameterizedType1, literal1.getType());
        assertEquals(parameterizedType1.hashCode(), literal1.hashCode());
        assertEquals(parameterizedType1,
                new ParameterizedTypeImpl(List.class, WildcardTypeImpl.defaultInstance()));

        // List<String>
        ParameterizedTypeImpl parameterizedType2 = new ParameterizedTypeImpl(List.class, String.class);
        TypeLiteral<List<String>> literal2 = new TypeLiteral<List<String>>() {
        };
        assertEquals(parameterizedType2,
                new ParameterizedTypeImpl(List.class, String.class));
        assertEquals(parameterizedType2, literal2.getType());
        assertEquals(parameterizedType2.hashCode(), literal2.getType().hashCode());

        // List<? extends Number>
        ParameterizedTypeImpl parameterizedType3 = new ParameterizedTypeImpl(List.class,
                WildcardTypeImpl.withUpperBound(Number.class));
        TypeLiteral<List<? extends Number>> literal3 = new TypeLiteral<List<? extends Number>>() {
        };
        assertEquals(parameterizedType3, literal3.getType());
        assertEquals(parameterizedType3.hashCode(), literal3.getType().hashCode());
        assertEquals(parameterizedType3.hashCode(), new ParameterizedTypeImpl(List.class,
                WildcardTypeImpl.withUpperBound(Number.class)).hashCode());
        assertNotEquals(parameterizedType3, parameterizedType1);
    }

}
