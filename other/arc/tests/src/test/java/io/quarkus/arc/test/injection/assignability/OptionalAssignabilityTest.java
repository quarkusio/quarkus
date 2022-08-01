package io.quarkus.arc.test.injection.assignability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
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
