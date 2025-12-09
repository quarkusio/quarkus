package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.inject.Any;

import org.junit.jupiter.api.Test;

public class AnnotationsTest {
    @Test
    public void test() {
        Set<Annotation> required = Set.of(Initialized.Literal.REQUEST, Any.Literal.INSTANCE);
        Set<Annotation> present = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        assertFalse(Annotations.areAllPresent(required, present, Map.of()));

        required = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        assertTrue(Annotations.areAllPresent(required, present, Map.of()));

        required = Set.of(Any.Literal.INSTANCE);
        assertTrue(Annotations.areAllPresent(required, present, Map.of()));

        required = Set.of(Initialized.Literal.APPLICATION);
        assertTrue(Annotations.areAllPresent(required, present, Map.of()));
    }
}
