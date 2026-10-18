package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class CustomGlobalScopeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .buildCompatibleExtensions(new MyExtension())
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("Cannot register custom context class"));
        assertTrue(error.getMessage().contains("because it is a built-in global scope"));
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(MetaAnnotations meta) {
            meta.addContext(Dependent.class, ExtraDependentContext.class);
        }
    }

    public static class ExtraDependentContext implements AlterableContext {
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
        }

        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return creationalContext != null ? contextual.create(creationalContext) : null;
        }

        public <T> T get(Contextual<T> contextual) {
            return null;
        }

        public boolean isActive() {
            return true;
        }

        public void destroy(Contextual<?> contextual) {
        }
    }
}
