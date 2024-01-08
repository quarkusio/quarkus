package io.quarkus.arc.test.contexts.request.optimized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class RequestContextInstancesTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Boom.class, Bim.class)
            .optimizeContexts(true)
            .build();

    @Test
    public void testContext() {
        ArcContainer container = Arc.container();
        container.requestContext().activate();

        InstanceHandle<Boom> handle = container.instance(Boom.class);
        Boom boom = handle.get();
        // ContextInstances#computeIfAbsent()
        String id1 = boom.ping();
        assertEquals(id1, boom.ping());

        // ContextInstances#remove()
        handle.destroy();
        // ContextInstances#getAllPresent()
        assertEquals(0, container.getActiveContext(RequestScoped.class).getState().getContextualInstances().size());

        // Init a new instance of Boom
        String id2 = boom.ping();
        assertNotEquals(id1, id2);
        assertEquals(id2, boom.ping());

        InjectableContext appContext = container.getActiveContext(RequestScoped.class);
        // ContextInstances#removeEach()
        appContext.destroy();
        assertNotEquals(id2, boom.ping());

        container.requestContext().terminate();
    }

    @RequestScoped
    public static class Boom {

        private String id;

        @Inject
        Bim bim;

        String ping() {
            return id;
        }

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<?> f = executorService.submit(() -> {
                // Force the init of the bean on a different thread
                bim.bam();
            });
            try {
                f.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new IllegalStateException(e);
            }
            executorService.shutdownNow();
        }

        @PreDestroy
        void destroy() {
            throw new IllegalStateException("Boom");
        }

    }

    @ApplicationScoped
    public static class Bim {

        public void bam() {
        }

    }
}
