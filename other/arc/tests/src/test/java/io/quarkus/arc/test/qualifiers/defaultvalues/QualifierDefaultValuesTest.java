package io.quarkus.arc.test.qualifiers.defaultvalues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
