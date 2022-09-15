package io.quarkus.arc.test.alternatives.priority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AlternativePriorityResolutionTest {
    @RegisterExtension
    ArcTestContainer testContainer = new ArcTestContainer(NoParentAlternativePriorityProducer1.class,
            NoParentAlternativePriorityProducer2.class, ParentAlternativePriorityProducer3.class,
            PrioritizedConsumer.class, MessageBean.class);

    @Test
    public void testAlternativePriorityResolution() {
        PrioritizedConsumer bean = Arc.container().instance(PrioritizedConsumer.class).get();
        assertNotNull(bean, "PrioritizedConsumer bean should not be null");
        assertEquals("jar", bean.ping());
    }

    @Singleton
    static class PrioritizedConsumer {
        @Inject
        MessageBean messageBean;

        String ping() {
            return messageBean.ping();
        }
    }

    @Singleton
    static class NoParentAlternativePriorityProducer1 {

        @Produces
        @Singleton
        @Alternative
        @io.quarkus.arc.Priority(4)
        public MessageBean createBar() {
            return new MessageBean("jar");
        };
    }

    @Singleton
    static class NoParentAlternativePriorityProducer2 {

        @Produces
        @Singleton
        @AlternativePriority(2)
        public MessageBean createBar() {
            return new MessageBean("far");
        }
    }

    @Singleton
    @Alternative
    @Priority(2)
    static class ParentAlternativePriorityProducer3 {
        @Produces
        @Singleton
        public MessageBean createBar() {
            return new MessageBean("car");
        }
    }

    @Vetoed
    static class MessageBean {
        final String msg;

        MessageBean(String msg) {
            this.msg = msg;
        }

        public String ping() {
            return msg;
        }
    }
}
