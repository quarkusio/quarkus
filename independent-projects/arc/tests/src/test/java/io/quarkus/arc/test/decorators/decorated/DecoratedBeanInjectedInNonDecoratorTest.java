package io.quarkus.arc.test.decorators.decorated;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Decorated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class DecoratedBeanInjectedInNonDecoratorTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(InvalidBean.class)
            .shouldFail()
            .build();

    @Test
    public void testDecoration() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().startsWith(
                "Invalid injection of @Decorated Bean<T>, can only be injected into decorators but was detected in: "
                        + InvalidBean.class.getName() + "#decorated"));
    }

    @ApplicationScoped
    static class InvalidBean {

        @Inject
        @Decorated
        Bean<?> decorated;

    }
}
