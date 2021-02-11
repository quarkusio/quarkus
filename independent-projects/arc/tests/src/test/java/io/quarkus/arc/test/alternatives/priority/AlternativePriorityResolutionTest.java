package io.quarkus.arc.test.alternatives.priority;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.annotation.Priority;
import javax.enterprise.inject.*;
import javax.inject.Inject;
import javax.inject.Singleton;
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
        @AlternativePriority(4)
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
