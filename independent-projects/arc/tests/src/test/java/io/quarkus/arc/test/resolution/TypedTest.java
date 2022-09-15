package io.quarkus.arc.test.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
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
        assertTrue(container.instance(MyOtherBean.class).isAvailable());
        boolean found = false;
        for (Bean<?> bean : container.beanManager().getBeans(Object.class)) {
            InjectableBean<?> injectable = (InjectableBean<?>) bean;
            if (injectable.getDeclaringBean() == null && injectable.getBeanClass().equals(MyOtherBean.class)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "MyOtherBean not found");
    }

    @Typed // -> bean types = [Object.class]
    @Singleton
    static class MyBean {

        void myObserver(@Observes String event) {
            EVENT.set(event);
        }

    }

    @Typed(MyOtherBean.class) // -> bean types = [MyOtherBean.class, Object.class]
    @Singleton
    static class MyOtherBean {

        // -> bean types = [Stage.class, Object.class]
        @Produces
        Stage myStage() {
            return new Stage("produced");
        }

    }

    @Typed // -> bean types = [Object.class]
    static class Stage {

        final String id;

        public Stage(String id) {
            this.id = id;
        }

    }

}
