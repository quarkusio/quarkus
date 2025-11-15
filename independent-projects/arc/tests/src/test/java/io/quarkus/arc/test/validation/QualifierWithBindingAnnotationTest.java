package io.quarkus.arc.test.validation;

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

public class QualifierWithBindingAnnotationTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Alpha.class, MyQualifier.class, SomeAnnotation.class).shouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Dependent
    @MyQualifier(@SomeAnnotation("foo"))
    static class Alpha {

    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
        SomeAnnotation value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SomeAnnotation {
        String value();
    }
}
