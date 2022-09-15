package io.quarkus.arc.test.producer.privatemember;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PrivateProducerMethodTest {

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

        private String name = null;

        @PostConstruct
        void init() {
            name = "foo";
        }

        @Produces
        private Head produce() {
            return new Head() {
                @Override
                public String name() {
                    return name;
                }
            };
        }

    }
}
