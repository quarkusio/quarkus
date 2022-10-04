package io.quarkus.arc.test.observers.metadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class EventMetadataWrongInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(WrongBean.class).shouldFail()
            .build();

    @Test
    public void testMetadata() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Singleton
    static class WrongBean {

        @Inject
        EventMetadata metadata;

    }

}
