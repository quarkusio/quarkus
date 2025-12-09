package io.quarkus.arc.test.validation;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Qualifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class QualifierWithBindingArrayTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Alpha.class, MyQualifier.class)
            .shouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("with array-valued return type"));
        assertTrue(error.getMessage().contains("have to be annotated with @jakarta.enterprise.util.Nonbinding"));
    }

    @Dependent
    @MyQualifier(first = { 1 }, second = { "foobar" })
    static class Alpha {

    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
        int[] first();

        String[] second();
    }
}
