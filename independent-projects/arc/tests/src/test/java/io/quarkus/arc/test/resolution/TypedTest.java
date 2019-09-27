package io.quarkus.arc.test.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TypedTest {

    static final AtomicReference<String> EVENT = new AtomicReference<String>();

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyOtherBean.class, Stage.class);

    @Test
    public void testEmptyTyped() throws IOException {
        ArcContainer container = Arc.container();
        assertFalse(container.instance(MyBean.class).isAvailable());
        assertNull(EVENT.get());
        container.beanManager().getEvent().fire("foo");
        assertEquals("foo", EVENT.get());
        InstanceHandle<Stage> stage = container.instance(Stage.class);
        assertTrue(stage.isAvailable());
        assertEquals("produced", stage.get().id);
    }

    @Typed // -> bean types = { Object.class }
    @Singleton
    static class MyBean {

        void myObserver(@Observes String event) {
            EVENT.set(event);
        }

    }

    @Singleton
    static class MyOtherBean {

        @Produces
        Stage myStage() {
            return new Stage("produced");
        }

    }

    @Typed
    static class Stage {

        final String id;

        public Stage(String id) {
            this.id = id;
        }

    }

}
