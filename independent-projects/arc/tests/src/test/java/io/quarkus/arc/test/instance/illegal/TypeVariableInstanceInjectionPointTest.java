package io.quarkus.arc.test.instance.illegal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TypeVariableInstanceInjectionPointTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Head.class).shouldFail().build();;

    @Test
    public void testError() {
        Throwable failure = container.getFailure();
        assertNotNull(failure);
        assertTrue(failure instanceof DefinitionException);
        assertEquals(
                "Type variable is not a legal type argument for jakarta.enterprise.inject.Instance: io.quarkus.arc.test.instance.illegal.TypeVariableInstanceInjectionPointTest$Head#instance",
                failure.getMessage());
    }

    @Dependent
    static class Head<T> {

        @Inject
        Instance<T> instance;

    }

}
