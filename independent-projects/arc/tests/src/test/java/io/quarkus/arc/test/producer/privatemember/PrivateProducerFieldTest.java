package io.quarkus.arc.test.producer.privatemember;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PrivateProducerFieldTest {

    @RegisterExtension
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
