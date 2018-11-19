package org.jboss.protean.arc.test.injection.assignability;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class OptionalAssignabilityTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(OptionalProducer.class, InjectOptionals.class);

    @Test
    public void testInjection() {
        assertEquals(Integer.valueOf(10), Arc.container().instance(InjectOptionals.class).get().getAge());
    }

    @ApplicationScoped
    static class OptionalProducer {

        @SuppressWarnings("unchecked")
        @Dependent
        @Produces
        <T> Optional<T> produceOptional(InjectionPoint injectionPoint) {
            return (Optional<T>) Optional.of(10);
        }

    }

    @ApplicationScoped
    static class InjectOptionals {

        private Integer age;

        @Inject
        private void setOptionals(Optional<Integer> age) {
            this.age = age.orElse(1);
        }

        Integer getAge() {
            return age;
        }

    }
}
