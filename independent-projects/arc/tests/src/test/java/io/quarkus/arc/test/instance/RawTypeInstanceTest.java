package io.quarkus.arc.test.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RawTypeInstanceTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Alpha.class).shouldFail().build();

    @Test
    public void testDefinitionError() {
        Throwable failure = container.getFailure();
        assertNotNull(failure);
        assertEquals(DefinitionException.class, failure.getClass());
        assertTrue(failure.getMessage().contains(Instance.class.getName()));
        assertTrue(failure.getMessage().contains("Alpha#instance"));
    }

    @Singleton
    static class Alpha {

        @SuppressWarnings("rawtypes")
        @Inject
        Instance instance;

    }

}
