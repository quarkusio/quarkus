package io.quarkus.arc.test.all;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;
import io.quarkus.arc.test.TestLiteral;

public class ListAllTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Service.class, ServiceAlpha.class, ServiceBravo.class,
            MyQualifier.class, Foo.class);

    @Test
    public void testSelectAll() {
        verifyHandleInjection(Arc.container().listAll(Service.class), Object.class);
    }

    // another set of tests is in io.quarkus.arc.test.lookup.ListInjectionTest
    @Test
    public void testInjectAllList() {
        Foo foo = Arc.container().select(Foo.class).get();
        // InstanceHandle variant
        verifyHandleInjection(foo.allHandles, Service.class);

        // plain list variant
        verifyInjection(foo.allServices, Service.class);
    }

    private void verifyHandleInjection(List<InstanceHandle<Service>> services, Class<?> expectedInjectionPointType) {
        // the behavior should be equivalent to @Inject @Any Instance<Service>
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
        assertEquals(expectedInjectionPointType, bravo.getInjectionPoint().get().getType());
        bravoHandle.destroy();
        assertEquals(true, ServiceBravo.DESTROYED.get());
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> bravoHandle.get());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Arc.container().listAll(Service.class, new TestLiteral()));
    }

    private void verifyInjection(List<Service> services, Class<?> expectedInjectionPointType) {
        assertEquals(2, services.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> services.remove(0));
        // ServiceBravo has higher priority
        Service bravo = services.get(0);
        assertEquals("bravo", bravo.ping());
        assertEquals("alpha", services.get(1).ping());
        assertTrue(bravo.getInjectionPoint().isPresent());
        // Empty injection point
        assertEquals(expectedInjectionPointType, bravo.getInjectionPoint().get().getType());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Arc.container().listAll(Service.class, new TestLiteral()));
    }

    @Singleton
    public static class Foo {
        @Inject
        @All
        List<Service> allServices;

        @Inject
        @All
        List<InstanceHandle<Service>> allHandles;

    }

    interface Service {

        String ping();

        default Optional<InjectionPoint> getInjectionPoint() {
            return Optional.empty();
        }

    }

    @Singleton
    static class ServiceAlpha implements Service {

        @Override
        public String ping() {
            return "alpha";
        }
    }

    @MyQualifier
    @Priority(5) // this impl should go first
    @Dependent
    static class ServiceBravo implements Service {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @Inject
        InjectionPoint injectionPoint;

        @Override
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
