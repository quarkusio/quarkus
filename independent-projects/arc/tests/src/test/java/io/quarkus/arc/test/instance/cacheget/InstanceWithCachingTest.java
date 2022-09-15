package io.quarkus.arc.test.instance.cacheget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.WithCaching;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InstanceWithCachingTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Client.class, Washcloth.class);

    @Test
    public void testDestroy() {
        Client client = Arc.container().instance(Client.class).get();
        assertEquals(client.nextId(), client.nextId());
        client.clear();
        assertTrue(Washcloth.DESTROYED.get());
    }

    @ApplicationScoped
    static class Client {

        @WithCaching
        @Inject
        Instance<Washcloth> instance;

        String nextId() {
            return instance.get().id;
        }

        void clear() {
            ((InjectableInstance<Washcloth>) instance).clearCache();
        }

    }

    @Dependent
    static class Washcloth {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        String id;

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

}
