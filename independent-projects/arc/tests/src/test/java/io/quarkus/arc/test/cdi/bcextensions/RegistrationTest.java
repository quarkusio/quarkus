package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class RegistrationTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyService.class, MyFooService.class, MyBarService.class, MyBarServiceProducer.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        assertEquals(2, MyExtension.beanCounter.get());
        assertEquals(1, MyExtension.beanMyQualifierCounter.get());
        assertEquals(1, MyExtension.observerQualifierCounter.get());
    }

    public static class MyExtension implements BuildCompatibleExtension {
        static final AtomicInteger beanCounter = new AtomicInteger();
        static final AtomicInteger beanMyQualifierCounter = new AtomicInteger();
        static final AtomicInteger observerQualifierCounter = new AtomicInteger();

        @Registration(types = MyService.class)
        public void beans(BeanInfo bean) {
            beanCounter.incrementAndGet();

            if (bean.qualifiers().stream().anyMatch(it -> it.name().equals(MyQualifier.class.getName()))) {
                beanMyQualifierCounter.incrementAndGet();
            }
        }

        @Registration(types = Object.class)
        public void observers(ObserverInfo observer, Types types) {
            if (observer.declaringClass().superInterfaces().contains(types.of(MyService.class))) {
                observerQualifierCounter.addAndGet(observer.qualifiers().size());
            }
        }
    }

    // ---

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MyQualifier {
    }

    public interface MyService {
        String hello();
    }

    @Singleton
    public static class MyFooService implements MyService {
        @Override
        public String hello() {
            return "foo";
        }

        void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        }
    }

    // intentionally not a bean, to test that producer-based bean is processed
    public static class MyBarService implements MyService {
        @Override
        public String hello() {
            return "bar";
        }
    }

    @Singleton
    public static class MyBarServiceProducer {
        @Produces
        @Singleton
        @MyQualifier
        public MyBarService produce() {
            return new MyBarService();
        }
    }
}
