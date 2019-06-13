package io.quarkus.arc.test.producer.privatemember;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.junit.Rule;
import org.junit.Test;

public class PrivateProducerFieldTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(HeadProducer.class);

    @Test
    public void testInjection() {
        assertEquals("foo", Arc.container().instance(Head.class).get().name());
    }

    static class Head {

        public String name() {
            return null;
        }

    }

    @ApplicationScoped
    static class HeadProducer {

        @Produces
        private Head head = new Head() {
            @Override
            public String name() {
                return "foo";
            }
        };

    }
}
