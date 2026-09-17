package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.InterceptorInfo;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class RegistrationTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyInterceptorBinding.class, MyInterceptor.class, MyService.class,
                    MyGenericService.class, MyFooService.class, MyBarService.class, MyBarServiceProducer.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        assertEquals(2, MyExtension.beanCounter.get());
        assertEquals(2, MyExtension.genericBeanCounter.get());
        assertEquals(1, MyExtension.beanMyQualifierCounter.get());
        assertEquals(4, MyExtension.observerCounter.get());
        assertEquals(1, MyExtension.observerQualifierCounter.get());
        assertEquals(2, MyExtension.genericObserverCounter.get()); // one observer counted twice
        assertEquals(4, MyExtension.rawObserverCounter.get()); // two observers, each counted twice
        assertEquals(2, MyExtension.interceptorCounter.get()); // one interceptor counted twice
    }

    public static class MyExtension implements BuildCompatibleExtension {
        static final AtomicInteger beanCounter = new AtomicInteger();
        static final AtomicInteger genericBeanCounter = new AtomicInteger();
        static final AtomicInteger beanMyQualifierCounter = new AtomicInteger();
        static final AtomicInteger observerCounter = new AtomicInteger();
        static final AtomicInteger observerQualifierCounter = new AtomicInteger();
        static final AtomicInteger rawObserverCounter = new AtomicInteger();
        static final AtomicInteger genericObserverCounter = new AtomicInteger();
        static final AtomicInteger interceptorCounter = new AtomicInteger();

        @Registration(types = MyService.class)
        public void beans(BeanInfo bean) {
            beanCounter.incrementAndGet();

            if (bean.qualifiers().stream().anyMatch(it -> it.name().equals(MyQualifier.class.getName()))) {
                beanMyQualifierCounter.incrementAndGet();
            }
        }

        @Registration(types = MyGenericServiceOfString.class)
        public void genericBeans(BeanInfo bean) {
            genericBeanCounter.incrementAndGet();
        }

        static class MyGenericServiceOfString extends TypeLiteral<MyGenericService<String>> {
        }

        @Registration(types = Object.class)
        public void observers(ObserverInfo observer, Types types) {
            if (observer.declaringClass().superInterfaces().contains(types.of(MyService.class))) {
                observerCounter.incrementAndGet();
                observerQualifierCounter.addAndGet(observer.qualifiers().size());
            }
        }

        @Registration(types = CollectionOfString.class)
        public void genericObservers1(ObserverInfo observer) {
            genericObserverCounter.incrementAndGet();
        }

        @Registration(types = ListOfString.class)
        public void genericObservers2(ObserverInfo observer) {
            genericObserverCounter.incrementAndGet();
        }

        @Registration(types = Collection.class)
        public void rawObservers1(ObserverInfo observer) {
            rawObserverCounter.incrementAndGet();
        }

        @Registration(types = List.class)
        public void rawObservers2(ObserverInfo observer) {
            rawObserverCounter.incrementAndGet();
        }

        static class CollectionOfString extends TypeLiteral<Collection<String>> {
        }

        static class ListOfString extends TypeLiteral<List<String>> {
        }

        @Registration(types = MyInterceptor.class)
        public void interceptors(InterceptorInfo interceptor) {
            interceptorCounter.incrementAndGet();
        }

        @Registration(types = MyInterceptor.class)
        public void interceptorsAsBeans(BeanInfo interceptor, Messages msg) {
            if (!interceptor.isInterceptor()) {
                msg.error("Interceptor expected", interceptor);
            }
            interceptorCounter.incrementAndGet();
        }
    }

    // ---

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MyQualifier {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @InterceptorBinding
    public @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    public static class MyInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }

    public interface MyService {
        String hello();
    }

    public interface MyGenericService<T> {
        T hello();
    }

    @Singleton
    public static class MyFooService implements MyService, MyGenericService<String> {
        @Override
        public String hello() {
            return "foo";
        }

        void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        }

        void observeListOfString(@Observes List<String> list) {
        }

        // this observer should _not_ be counted among the generic observers
        void observeIterableOfString(@Observes Iterable<String> list) {
        }

        // this observer should _not_ be counted among the generic observers either
        void observeListOfInteger(@Observes List<Integer> list) {
        }
    }

    // intentionally not a bean, to test that producer-based bean is processed
    public static class MyBarService implements MyService, MyGenericService<String> {
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
