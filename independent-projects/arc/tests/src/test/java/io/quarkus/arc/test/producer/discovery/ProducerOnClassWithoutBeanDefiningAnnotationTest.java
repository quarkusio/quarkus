package io.quarkus.arc.test.producer.discovery;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.inject.Produces;
import org.junit.Rule;
import org.junit.Test;

public class ProducerOnClassWithoutBeanDefiningAnnotationTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringProducerMethod.class, IntegerProducerField.class);

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
