package io.quarkus.arc.test.all;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ListAllTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Service.class, ServiceAlpha.class, ServiceBravo.class);

    @Test
    public void testSelectAll() {
        List<InstanceHandle<Service>> services = Arc.container().listAll(Service.class);
        assertEquals(2, services.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> services.remove(0));
        // ServiceBravo has higher priority
        InstanceHandle<Service> bravoHandle = services.get(0);
        Service bravo = bravoHandle.get();
        assertEquals("bravo", bravo.ping());
        assertEquals("alpha", services.get(1).get().ping());
        assertEquals(Dependent.class, bravoHandle.getBean().getScope());
        assertTrue(bravo.getInjectionPoint().isPresent());
        // Empty injection point
        assertEquals(Object.class, bravo.getInjectionPoint().get().getType());
        bravoHandle.destroy();
        assertEquals(true, ServiceBravo.DESTROYED.get());
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> bravoHandle.get());
    }

    interface Service {

        String ping();

        default Optional<InjectionPoint> getInjectionPoint() {
            return Optional.empty();
        }

    }

    @Singleton
    static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    @Priority(5) // this impl should go first
    @Dependent
    static class ServiceBravo implements Service {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @Inject
        InjectionPoint injectionPoint;

        public String ping() {
            return "bravo";
        }

        @Override
        public Optional<InjectionPoint> getInjectionPoint() {
            return Optional.of(injectionPoint);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

}
