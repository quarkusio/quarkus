package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.inject.Any;

import org.junit.jupiter.api.Test;

public class QualifiersTest {

    @Test
    public void testIsSubset() {
        Qualifiers qualifiers = new Qualifiers(Collections.emptySet(), Collections.emptyMap());
        Set<Annotation> observed = Set.of(Initialized.Literal.REQUEST, Any.Literal.INSTANCE);
        Set<Annotation> event = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        assertFalse(qualifiers.isSubset(observed, event));

        observed = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        assertTrue(qualifiers.isSubset(observed, event));

        observed = Set.of(Any.Literal.INSTANCE);
        assertTrue(qualifiers.isSubset(observed, event));

        observed = Set.of(Initialized.Literal.APPLICATION);
        assertTrue(qualifiers.isSubset(observed, event));
    }

}
