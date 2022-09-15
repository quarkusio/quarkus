package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.inject.Any;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class QualifiersTest {

    @Test
    public void testIsSubset() {
        Set<Annotation> observed = Set.of(Initialized.Literal.REQUEST, Any.Literal.INSTANCE);
        Set<Annotation> event = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        assertFalse(Qualifiers.isSubset(observed, event, Collections.emptyMap()));

        observed = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        assertTrue(Qualifiers.isSubset(observed, event, Collections.emptyMap()));

        observed = Set.of(Any.Literal.INSTANCE);
        assertTrue(Qualifiers.isSubset(observed, event, Collections.emptyMap()));

        observed = Set.of(Initialized.Literal.APPLICATION);
        assertTrue(Qualifiers.isSubset(observed, event, Collections.emptyMap()));
    }

}
