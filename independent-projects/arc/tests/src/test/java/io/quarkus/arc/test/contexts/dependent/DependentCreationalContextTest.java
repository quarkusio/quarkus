package io.quarkus.arc.test.contexts.dependent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.impl.InstanceImpl;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DependentCreationalContextTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(NoPreDestroy.class, HasDestroy.class, HasDependency.class,
                    ProducerNoDisposer.class, ProducerWithDisposer.class, String.class, Boolean.class)
            .beanRegistrars(new BeanRegistrar() {

                @Override
                public void register(RegistrationContext context) {
                    context.configure(SyntheticOne.class).addType(SyntheticOne.class).creator(SyntheticOne.class).done();
                    context.configure(SyntheticTwo.class).addType(SyntheticTwo.class).creator(SyntheticTwo.class)
                            .destroyer(SyntheticTwo.class).done();
                }
            })
            .build();

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
        // Synthetic bean
        assertBeanType(instance, SyntheticOne.class, false);
        // Synthetic bean with destruction logic
        assertBeanType(instance, SyntheticTwo.class, true);
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

    public static class SyntheticOne implements BeanCreator<SyntheticOne> {

        @Override
        public SyntheticOne create(CreationalContext<SyntheticOne> creationalContext, Map<String, Object> params) {
            return new SyntheticOne();
        }

    }

    public static class SyntheticTwo implements BeanCreator<SyntheticTwo>, BeanDestroyer<SyntheticTwo> {

        @Override
        public SyntheticTwo create(CreationalContext<SyntheticTwo> creationalContext, Map<String, Object> params) {
            return new SyntheticTwo();
        }

        @Override
        public void destroy(SyntheticTwo instance, CreationalContext<SyntheticTwo> creationalContext,
                Map<String, Object> params) {
        }

    }
}
