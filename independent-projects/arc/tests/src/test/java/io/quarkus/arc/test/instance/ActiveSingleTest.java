package io.quarkus.arc.test.instance;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.test.ArcTestContainer;

public class ActiveSingleTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Service.class, Consumer.class)
            .beanRegistrars(context -> {
                context.configure(ServiceAlpha.class)
                        .types(Service.class, ServiceAlpha.class)
                        .scope(Singleton.class)
                        .checkActive(AlwaysActive.class)
                        .creator(AlphaCreator.class)
                        .done();

                context.configure(ServiceBravo.class)
                        .types(Service.class, ServiceBravo.class)
                        .scope(Singleton.class)
                        .checkActive(NeverActive.class)
                        .creator(BravoCreator.class)
                        .done();
            })
            .build();

    @Test
    public void testListActive() {
        Consumer consumer = Arc.container().select(Consumer.class).get();

        List<Service> activeServices = consumer.services.listActive();
        assertEquals(1, activeServices.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> activeServices.remove(0));
        Service alpha = activeServices.get(0);
        assertEquals("alpha", alpha.ping());
    }

    @Test
    public void testGetActive() {
        Consumer consumer = Arc.container().select(Consumer.class).get();

        assertEquals("alpha", consumer.services.getActive().ping());
    }

    @Singleton
    public static class Consumer {
        @Inject
        @Any
        InjectableInstance<Service> services;
    }

    interface Service {
        String ping();
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
        @Override
        public String ping() {
            return "bravo";
        }
    }

    static class BravoCreator implements BeanCreator<ServiceBravo> {
        @Override
        public ServiceBravo create(SyntheticCreationalContext<ServiceBravo> context) {
            return new ServiceBravo();
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
