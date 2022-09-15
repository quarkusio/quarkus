package io.quarkus.arc.test.producer.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.Produces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ProducerOnClassWithoutBeanDefiningAnnotationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringProducerMethod.class,
            IntegerProducerField.class);

    @Test
    public void testObserver() {
        assertEquals("foo", Arc.container().instance(String.class).get());
        assertEquals(Integer.valueOf(10), Arc.container().instance(Integer.class).get());
    }

    static class StringProducerMethod {

        @Produces
        String observeString() {
            return "foo";
        }

    }

    static class IntegerProducerField {

        @Produces
        Integer foo = 10;

    }

}
