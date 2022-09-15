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

public class StereotypeOnMethodTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(BeanWithStereotypeOnMethod.class, Audited.class)
            .shouldFail()
            .build();

    /**
     * Verify that DefinitionException is thrown if there is a stereotype applied on a non-producer method.
     */
    @Test
    public void testStereotypeOnNonProducerMethod() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
        assertTrue(error.getMessage().contains("auditedMethod"));
    }

    @ApplicationScoped
    static class BeanWithStereotypeOnMethod {

        @Audited
        public void auditedMethod() {

        }

    }

    @Stereotype
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface Audited {

    }

}
