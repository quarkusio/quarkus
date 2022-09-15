package io.quarkus.arc.test.observers.ifexists;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ReceptionIfExistsTest {

    static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(DependentObserver.class, RequestScopedObserver.class);

    @Test
    public void testObserver() {
        ArcContainer container = Arc.container();
        container.beanManager().getEvent().fire("foo");
        assertEquals(1, EVENTS.size());
        assertEquals(DependentObserver.class.getName() + "foo", EVENTS.get(0));

        // Activate the request context but the instance still does not exist
        EVENTS.clear();
        container.requestContext().activate();
        container.beanManager().getEvent().fire("foo");
        assertEquals(1, EVENTS.size());
        assertEquals(DependentObserver.class.getName() + "foo", EVENTS.get(0));
        container.requestContext().deactivate();

        // Activate the request context and the instance exists
        EVENTS.clear();
        container.requestContext().activate();
        // Force bean instance creation
        container.instance(RequestScopedObserver.class).get().ping();
        container.beanManager().getEvent().fire("foo");
        assertEquals(2, EVENTS.size());
        assertEquals(RequestScopedObserver.class.getName() + "foo", EVENTS.get(0));
        assertEquals(DependentObserver.class.getName() + "foo", EVENTS.get(1));
        container.requestContext().deactivate();
    }

    @RequestScoped
    static class RequestScopedObserver {

        void ping() {
        }

        void observeString(@Priority(1) @Observes(notifyObserver = Reception.IF_EXISTS) String value) {
            EVENTS.add(RequestScopedObserver.class.getName() + value);
        }

    }

    @Dependent
    static class DependentObserver {

        void observeString(@Priority(2) @Observes String value) {
            EVENTS.add(DependentObserver.class.getName() + value);
        }

    }

}
