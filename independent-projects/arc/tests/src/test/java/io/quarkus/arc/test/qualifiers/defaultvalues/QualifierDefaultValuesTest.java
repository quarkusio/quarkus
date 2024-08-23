package io.quarkus.arc.test.qualifiers.defaultvalues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class QualifierDefaultValuesTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Consumer.class, Animal.class, AnimalQualifier.class, Cat.class,
            Owl.class);

    @Test
    public void testDefaultValues() {
        Consumer consumer = Arc.container().instance(Consumer.class).get();
        assertEquals(2, consumer.animal.noOfLeg());
    }

    @Dependent
    public static class Consumer {

        @Inject
        @AnimalQualifier
        Animal animal;

    }

}
