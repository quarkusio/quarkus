package org.jboss.protean.arc.test.producer.privatemember;

import static org.junit.Assert.assertEquals;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class PrivateProducerMethodTest {

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

        private String name = null;

        @PostConstruct
        void init() {
            name = "foo";
        }

        @Produces
        private Head produce() {
            return new Head() {
                public String name() {
                    return name;
                }
            };
        }

    }
}
