package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class MonstrousLookupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyDependency.class, MyInterface.class, MyInterfaceImpl1.class, MyInterfaceImpl2.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method)
                        .withInstanceLookup()
                        .withArgumentLookup(0)
                        .withArgumentLookup(1)
                        .withArgumentLookup(2)
                        .withArgumentLookup(3)
                        .withArgumentLookup(4)
                        .build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<MyService, String> invoker = helper.getInvoker("hello");
        assertEquals("foobar0_1_MyDependency__2", invoker.invoke(null, new Object[5]));
        assertEquals(List.of("foo", "bar", "baz"), MyService.observed);
        assertEquals(2, MyService.listAll.size());
        assertEquals(1, MyService.listAll.stream().filter(it -> it instanceof MyInterfaceImpl1).count());
        assertEquals(1, MyService.listAll.stream().filter(it -> it instanceof MyInterfaceImpl2).count());

        assertEquals(3, MyDependency.CREATED);
        // - the looked up instance is destroyed by the invoker
        // - the instance obtained from the looked up `Instance` is destroyed transitively
        //   by the invoker destroying the `Instance`
        // - the instance obtained from `BeanManager.createInstance()` is destroyed manually
        assertEquals(3, MyDependency.DESTROYED);
    }

    @Singleton
    static class MyService {
        static final List<String> observed = new ArrayList<>();
        static final List<MyInterface> listAll = new ArrayList<>();

        public String hello(MyDependency dependency, Instance<MyDependency> instanceOfDependency,
                Event<List<String>> event, BeanManager beanManager, @All List<MyInterface> list) {
            Class<?> dependency1Class = instanceOfDependency.getHandle().getBean().getBeanClass();
            MyDependency dependency1 = instanceOfDependency.get();

            event.fire(List.of("foo", "bar"));

            Instance<Object> instance = beanManager.createInstance();
            MyDependency dependency2 = instance.select(MyDependency.class).get();
            int dependency2Id = dependency2.getId();
            instance.destroy(dependency2);

            beanManager.getEvent().select(new TypeLiteral<List<String>>() {
            }).fire(List.of("baz"));

            listAll.addAll(list);

            return "foobar" + dependency.getId() + "_" + dependency1.getId() + "_" + dependency1Class.getSimpleName()
                    + "__" + dependency2Id;
        }

        public void observe(@Observes List<String> event) {
            observed.addAll(event);
        }
    }

    @Dependent
    static class MyDependency {
        static int CREATED = 0;

        static int DESTROYED = 0;

        private int id;

        @PostConstruct
        public void init() {
            id = CREATED++;
        }

        @PreDestroy
        public void destroy() {
            DESTROYED++;
        }

        public int getId() {
            return id;
        }
    }

    interface MyInterface {
    }

    @Singleton
    static class MyInterfaceImpl1 implements MyInterface {
    }

    @Singleton
    static class MyInterfaceImpl2 implements MyInterface {
    }
}
