package io.quarkus.arc.test.observers.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RequestInObserverNotificationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(RequestFoo.class, MyObserver.class);

    @Test
    public void testObserverNotification() {
        ArcContainer container = Arc.container();
        AtomicReference<String> msg = new AtomicReference<String>();
        RequestFoo.DESTROYED.set(false);

        // Request context should be activated automatically
        container.beanManager().getEvent().select(AtomicReference.class).fire(msg);
        String fooId1 = msg.get();
        assertNotNull(fooId1);
        assertTrue(RequestFoo.DESTROYED.get());

        RequestFoo.DESTROYED.set(false);
        msg.set(null);

        ManagedContext requestContext = container.requestContext();
        assertFalse(requestContext.isActive());
        try {
            requestContext.activate();
            String fooId2 = container.instance(RequestFoo.class).get().getId();
            assertNotEquals(fooId1, fooId2);
            container.beanManager().getEvent().select(AtomicReference.class).fire(msg);
            assertEquals(fooId2, msg.get());
        } finally {
            requestContext.terminate();
        }
        assertTrue(RequestFoo.DESTROYED.get());
    }

    @Singleton
    static class MyObserver {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        void observeString(@Observes AtomicReference value, RequestFoo foo) {
            // does not trigger ContextNotActiveException
            value.set(foo.getId());
        }

    }

    @RequestScoped
    static class RequestFoo {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        public String getId() {
            return id;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }
    }

}
