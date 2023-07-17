package io.quarkus.arc.test.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.test.ArcTestContainer;
import io.smallrye.mutiny.Multi;

public class InvalidPreDestroyTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(InvalidBean.class).shouldFail().build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
        Assertions.assertTrue(error.getMessage().contains("invalid()"));
        Assertions.assertTrue(error.getMessage().contains("$InvalidBean"));
        Assertions.assertTrue(error.getMessage().contains("PreDestroy"));
    }

    @ApplicationScoped
    @Unremovable
    public static class InvalidBean {

        @PreDestroy
        public Multi<Void> invalid() {
            return null;
        }
    }
}
