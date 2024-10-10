package io.quarkus.arc.test.all;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Active;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;
import io.quarkus.arc.test.TestLiteral;

public class ListActiveTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Service.class, MyQualifier.class, Consumer.class)
            .beanRegistrars(context -> {
                context.configure(ServiceAlpha.class)
                        .types(Service.class, ServiceAlpha.class)
                        .scope(Singleton.class)
                        .checkActive(AlwaysActive.class)
                        .creator(AlphaCreator.class)
                        .done();

                context.configure(ServiceBravo.class)
                        .types(Service.class, ServiceBravo.class)
                        .qualifiers(AnnotationInstance.builder(MyQualifier.class).build())
                        .scope(Dependent.class)
                        .priority(5)
                        .addInjectionPoint(ClassType.create(InjectionPoint.class))
                        .checkActive(NeverActive.class)
                        .creator(BravoCreator.class)
                        .destroyer(BravoDestroyer.class)
                        .done();

                context.configure(ServiceCharlie.class)
                        .types(Service.class, ServiceCharlie.class)
                        .scope(Singleton.class)
                        .checkActive(NeverActive.class)
                        .creator(CharlieCreator.class)
                        .done();

                context.configure(ServiceDelta.class)
                        .types(Service.class, ServiceDelta.class)
                        .qualifiers(AnnotationInstance.builder(MyQualifier.class).build())
                        .scope(Dependent.class)
                        .priority(10)
                        .addInjectionPoint(ClassType.create(InjectionPoint.class))
                        .checkActive(AlwaysActive.class)
                        .creator(DeltaCreator.class)
                        .destroyer(DeltaDestroyer.class)
                        .done();
            })
            .build();

    @Test
    public void testSelectAll() {
        verifyHandleInjection(Arc.container().listActive(Service.class), Object.class);
    }

    @Test
    public void testInjectAllList() {
        Consumer consumer = Arc.container().select(Consumer.class).get();
        verifyHandleInjection(consumer.activeHandles, Service.class);
        verifyInjection(consumer.activeServices, Service.class);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Arc.container().listActive(Service.class, new TestLiteral()));
    }

    private void verifyHandleInjection(List<InstanceHandle<Service>> services, Class<?> expectedInjectionPointType) {
        assertEquals(2, services.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> services.remove(0));
        // ServiceDelta has higher priority
        InstanceHandle<Service> deltaHandle = services.get(0);
        Service delta = deltaHandle.get();
        assertEquals("delta", delta.ping());
        assertEquals("alpha", services.get(1).get().ping());
        assertEquals(Dependent.class, deltaHandle.getBean().getScope());
        assertNotNull(delta.getInjectionPoint());
        assertEquals(expectedInjectionPointType, delta.getInjectionPoint().getType());
        deltaHandle.destroy();
        assertTrue(DeltaDestroyer.DESTROYED);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(deltaHandle::get);
    }

    private void verifyInjection(List<Service> services, Class<?> expectedInjectionPointType) {
        assertEquals(2, services.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> services.remove(0));
        // ServiceDelta has higher priority
        Service delta = services.get(0);
        assertEquals("delta", delta.ping());
        assertEquals("alpha", services.get(1).ping());
        assertNotNull(delta.getInjectionPoint());
        assertEquals(expectedInjectionPointType, delta.getInjectionPoint().getType());
    }

    @Singleton
    public static class Consumer {
        @Inject
        @Active
        List<Service> activeServices;

        @Inject
        @Active
        List<InstanceHandle<Service>> activeHandles;
    }

    interface Service {
        String ping();

        default InjectionPoint getInjectionPoint() {
            return null;
        }
    }

    static class ServiceAlpha implements Service {
        public String ping() {
            return "alpha";
        }
    }

    static class AlphaCreator implements BeanCreator<ServiceAlpha> {
        @Override
        public ServiceAlpha create(SyntheticCreationalContext<ServiceAlpha> context) {
            return new ServiceAlpha();
        }
    }

    static class ServiceBravo implements Service {
        private final InjectionPoint injectionPoint;

        ServiceBravo(InjectionPoint injectionPoint) {
            this.injectionPoint = injectionPoint;
        }

        public String ping() {
            return "bravo";
        }

        @Override
        public InjectionPoint getInjectionPoint() {
            return injectionPoint;
        }
    }

    static class BravoCreator implements BeanCreator<ServiceBravo> {
        @Override
        public ServiceBravo create(SyntheticCreationalContext<ServiceBravo> context) {
            InjectionPoint ip = context.getInjectedReference(InjectionPoint.class);
            return new ServiceBravo(ip);
        }
    }

    static class BravoDestroyer implements BeanDestroyer<ServiceBravo> {
        static boolean DESTROYED = false;

        @Override
        public void destroy(ServiceBravo instance, CreationalContext<ServiceBravo> creationalContext,
                Map<String, Object> params) {
            DESTROYED = true;
        }
    }

    static class ServiceCharlie implements Service {
        @Override
        public String ping() {
            return "charlie";
        }
    }

    static class CharlieCreator implements BeanCreator<ServiceCharlie> {
        @Override
        public ServiceCharlie create(SyntheticCreationalContext<ServiceCharlie> context) {
            return new ServiceCharlie();
        }
    }

    static class ServiceDelta implements Service {
        private final InjectionPoint injectionPoint;

        ServiceDelta(InjectionPoint injectionPoint) {
            this.injectionPoint = injectionPoint;
        }

        @Override
        public String ping() {
            return "delta";
        }

        @Override
        public InjectionPoint getInjectionPoint() {
            return injectionPoint;
        }
    }

    static class DeltaCreator implements BeanCreator<ServiceDelta> {
        @Override
        public ServiceDelta create(SyntheticCreationalContext<ServiceDelta> context) {
            InjectionPoint ip = context.getInjectedReference(InjectionPoint.class);
            return new ServiceDelta(ip);
        }
    }

    static class DeltaDestroyer implements BeanDestroyer<ServiceDelta> {
        static boolean DESTROYED = false;

        @Override
        public void destroy(ServiceDelta instance, CreationalContext<ServiceDelta> creationalContext,
                Map<String, Object> params) {
            DESTROYED = true;
        }
    }

    static class AlwaysActive implements Supplier<ActiveResult> {
        @Override
        public ActiveResult get() {
            return ActiveResult.active();
        }
    }

    static class NeverActive implements Supplier<ActiveResult> {
        @Override
        public ActiveResult get() {
            return ActiveResult.inactive("");
        }
    }
}
