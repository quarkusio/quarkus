package io.quarkus.arc.test.lookup;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Priority;
import io.quarkus.test.QuarkusUnitTest;

public class ListInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, ServiceAlpha.class, ServiceBravo.class, ServiceCharlie.class, Service.class,
                            Counter.class, Converter.class, ConverterAlpha.class, ConverterBravo.class, MyQualifier.class));

    @Inject
    Foo foo;

    @Test
    public void testInjection() {
        // The list is prefetched eagerly, the container attempts to resolve ambiguities
        assertEquals(2, foo.services.size());
        // The list is immutable
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> foo.services.add(new ServiceAlpha()));
        // ServiceBravo has higher priority
        assertEquals("bravo", foo.services.get(0).ping());
        for (Service service : foo.services) {
            Optional<InjectionPoint> ip = service.getInjectionPoint();
            if (ip.isPresent()) {
                assertEquals(Foo.class, ip.get().getBean().getBeanClass());
                assertEquals(Service.class, ip.get().getType());
            }
        }
        // The list is empty if no beans are found
        assertTrue(foo.counters.isEmpty());

        assertEquals(1, foo.convertersDefault.size());
        assertEquals("ok", foo.convertersDefault.get(0).convert("Ok"));

        // Test constructor injection and additional qualifier
        assertEquals(1, foo.convertersMyQualifier.size());
        assertEquals("OK", foo.convertersMyQualifier.get(0).convert("OK"));
        assertEquals(1, foo.convertersMyQualifierField.size());
        assertEquals("OK", foo.convertersMyQualifierField.get(0).convert("OK"));

        // Test List<InstanceHandle<?>>
        assertEquals(1, foo.counterHandles.size());
        InstanceHandle<Counter> handle = foo.counterHandles.get(0);
        assertEquals(CounterAlpha.class, handle.getBean().getBeanClass());
        assertEquals(1, handle.get().count());
        handle.destroy();
        assertTrue(CounterAlpha.DESTROYED.get());
    }

    @Test
    public void testListAll() {
        List<InstanceHandle<Service>> services = Arc.container().listAll(Service.class);
        assertEquals(2, services.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> services.remove(0));
        // ServiceBravo has higher priority
        InstanceHandle<Service> bravoHandle = services.get(0);
        Service bravo = bravoHandle.get();
        assertEquals("bravo", bravo.ping());
        assertEquals(Dependent.class, bravoHandle.getBean().getScope());
        assertTrue(bravo.getInjectionPoint().isPresent());
        // Empty injection point
        assertEquals(Object.class, bravo.getInjectionPoint().get().getType());
    }

    @Singleton
    static class Foo {

        @Inject
        @All
        List<Service> services;

        @MyQualifier
        @All
        List<Counter> counters;

        @All
        List<InstanceHandle<Counter>> counterHandles;

        @Inject
        @All
        @Default
        List<Converter> convertersDefault;

        final List<Converter> convertersMyQualifier;

        @Inject
        @All
        @MyQualifier
        List<Converter> convertersMyQualifierField;

        Foo(@All @MyQualifier List<Converter> convertersMyQualifier) {
            this.convertersMyQualifier = convertersMyQualifier;
        }

    }

    interface Service {

        String ping();

        default Optional<InjectionPoint> getInjectionPoint() {
            return Optional.empty();
        }

    }

    interface Counter {

        int count();

    }

    interface Converter {

        String convert(String val);

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

        @Inject
        InjectionPoint injectionPoint;

        public String ping() {
            return "bravo";
        }

        @Override
        public Optional<InjectionPoint> getInjectionPoint() {
            return Optional.of(injectionPoint);
        }

    }

    @Dependent
    @Alternative // -> not enabled
    static class ServiceCharlie implements Service {

        public String ping() {
            return "charlie";
        }
    }

    @Dependent
    static class ConverterAlpha implements Converter {

        @Override
        public String convert(String val) {
            return val.toLowerCase();
        }

    }

    @MyQualifier
    @Singleton
    static class ConverterBravo implements Converter {

        @Override
        public String convert(String val) {
            return val.toUpperCase();
        }

    }

    @Dependent
    static class CounterAlpha implements Counter {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        @Override
        public int count() {
            return 1;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

}
