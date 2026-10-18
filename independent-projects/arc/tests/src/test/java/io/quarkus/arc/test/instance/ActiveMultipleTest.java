package io.quarkus.arc.test.instance;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;

public class ActiveMultipleTest {
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
    public void testListActive() {
        Consumer consumer = Arc.container().select(Consumer.class).get();

        List<Service> activeServices = consumer.services.listActive();
        assertEquals(2, activeServices.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> activeServices.remove(0));
        // ServiceDelta has higher priority
        Service delta = activeServices.get(0);
        assertEquals("delta", delta.ping());
        assertEquals("alpha", activeServices.get(1).ping());
        assertNotNull(delta.getInjectionPoint());
        assertEquals(Service.class, delta.getInjectionPoint().getType());
    }

    @Test
    public void testGetActive() {
        Consumer consumer = Arc.container().select(Consumer.class).get();

        assertThrows(AmbiguousResolutionException.class, () -> {
            consumer.services.getActive();
        });
    }

    @Singleton
    public static class Consumer {
        @Inject
        @Any
        InjectableInstance<Service> services;
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
