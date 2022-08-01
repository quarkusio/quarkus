package io.quarkus.arc.test.injection.constructornoinject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultiInjectConstructorFailureTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(CombineHarvester.class, Head.class)
            .shouldFail()
            .build();

    @Test
    public void testInjection() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Dependent
    static class Head {

    }

    @Singleton
    static class CombineHarvester {

        Head head;

        @Inject
        public CombineHarvester() {
            this.head = null;
        }

        @Inject
        public CombineHarvester(Head head) {
            this.head = head;
        }

    }
}
