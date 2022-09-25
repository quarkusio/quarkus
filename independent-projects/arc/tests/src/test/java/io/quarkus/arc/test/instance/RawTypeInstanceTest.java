package io.quarkus.arc.test.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

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
