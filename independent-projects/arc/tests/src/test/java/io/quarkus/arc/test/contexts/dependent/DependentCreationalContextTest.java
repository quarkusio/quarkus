package io.quarkus.arc.test.contexts.dependent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.InstanceImpl;
import io.quarkus.arc.test.ArcTestContainer;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DependentCreationalContextTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(NoPreDestroy.class, HasDestroy.class, HasDependency.class,
            ProducerNoDisposer.class, ProducerWithDisposer.class, String.class, Boolean.class);

    @Test
    public void testCreationalContextOptimization() {
        InstanceImpl<Object> instance = (InstanceImpl<Object>) Arc.container().beanManager().createInstance();
        assertBeanType(instance, NoPreDestroy.class, false);
        assertBeanType(instance, HasDestroy.class, true);
        assertBeanType(instance, HasDependency.class, true);
        // ProducerNoDisposer
        assertBeanType(instance, boolean.class, false);
        // ProducerWithDisposer
        assertBeanType(instance, String.class, true);
    }

    <T> void assertBeanType(InstanceImpl<Object> instance, Class<T> beanType, boolean shouldBeStored) {
        T bean = instance.select(beanType).get();
        assertNotNull(bean);
        if (shouldBeStored) {
            assertTrue(instance.hasDependentInstances());
        } else {
            assertFalse(instance.hasDependentInstances());
        }
        instance.destroy(bean);
    }

    @Dependent
    static class NoPreDestroy {

    }

    @Dependent
    static class HasDestroy {

        @PreDestroy
        void destroy() {
        }

    }

    @Dependent
    static class HasDependency {

        @Inject
        HasDestroy dep;

    }

    @Dependent
    static class ProducerNoDisposer {

        @Produces
        Boolean ping() {
            return true;
        }

    }

    @Dependent
    static class ProducerWithDisposer {

        @Produces
        String ping() {
            return "ok";
        }

        void dispose(@Disposes String ping) {
        }

    }
}
