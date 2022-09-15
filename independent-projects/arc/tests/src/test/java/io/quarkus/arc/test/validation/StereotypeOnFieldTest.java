package io.quarkus.arc.test.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.DefinitionException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StereotypeOnFieldTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(BeanWithStereotypeOnField.class, Audited.class)
            .shouldFail()
            .build();

    /**
     * Verify that DefinitionException is thrown if there is a stereotype applied on a non-producer field.
     */
    @Test
    public void testStereotypeOnNonProducerField() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
        assertTrue(error.getMessage().contains("auditedField"));
    }

    @ApplicationScoped
    static class BeanWithStereotypeOnField {

        @Audited
        private String auditedField;

    }

    @Stereotype
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface Audited {

    }

}
