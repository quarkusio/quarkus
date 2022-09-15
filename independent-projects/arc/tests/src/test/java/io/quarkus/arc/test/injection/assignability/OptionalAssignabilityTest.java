package io.quarkus.arc.test.injection.assignability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class OptionalAssignabilityTest {

    @RegisterExtension
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
