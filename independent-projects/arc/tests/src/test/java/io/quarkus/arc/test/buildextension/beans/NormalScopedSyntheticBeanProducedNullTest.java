package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedSyntheticBeanProducedNullTest {

    public static volatile boolean beanDestroyerInvoked = false;

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanRegistrars(new TestRegistrar()).build();

    @Test
    public void testCreationException() {
        CreationException e = assertThrows(CreationException.class, () -> {
            Arc.container().instance(CharSequence.class).get().length();
        });
        assertTrue(e.getMessage().contains("Null contextual instance was produced by a normal scoped synthetic bean"),
                e.getMessage());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(CharSequence.class).types(CharSequence.class).unremovable().scope(ApplicationScoped.class)
                    .creator(CharSequenceCreator.class).done();
        }

    }

    public static class CharSequenceCreator implements BeanCreator<CharSequence> {

        @Override
        public CharSequence create(CreationalContext<CharSequence> creationalContext, Map<String, Object> params) {
            return null;
        }

    }

}
