package io.quarkus.arc.test.contexts.application.optimized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class ApplicationContextInstancesTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Boom.class)
            .optimizeContexts(true)
            .build();

    @Test
    public void testContext() {
        ArcContainer container = Arc.container();
        InstanceHandle<Boom> handle = container.instance(Boom.class);
        Boom boom = handle.get();
        String id1 = boom.ping();
        assertEquals(id1, boom.ping());

        handle.destroy();
        String id2 = boom.ping();
        assertNotEquals(id1, id2);
        assertEquals(id2, boom.ping());

        InjectableContext appContext = container.getActiveContext(ApplicationScoped.class);
        appContext.destroy();
        assertNotEquals(id2, boom.ping());
    }

    @ApplicationScoped
    public static class Boom {

        private String id;

        String ping() {
            return id;
        }

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

    }
}
