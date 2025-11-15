package io.quarkus.arc.test.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticQualifierWithBindingArrayTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(ToBeQualifier.class, Alpha.class)
            .qualifierRegistrars(new QualifierRegistrar() {

                @Override
                public Map<DotName, Set<String>> getAdditionalQualifiers() {
                    Map<DotName, Set<String>> qualifiers = new HashMap<>();
                    qualifiers.put(DotName.createSimple(ToBeQualifier.class.getName()), Collections.emptySet());
                    return qualifiers;
                }
            })
            .shouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Dependent
    @ToBeQualifier(first = { 1 }, second = { "foobar" })
    static class Alpha {

    }

    // not marked as @Qualifier, instead it is added via registrar
    @Retention(RetentionPolicy.RUNTIME)
    @interface ToBeQualifier {
        int[] first();

        String[] second();
    }
}
