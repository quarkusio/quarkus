package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class ArgumentLookupDependentTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyDependency.class, MyTransitiveDependency.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method)
                        .withArgumentLookup(0)
                        .build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> invoker = helper.getInvoker("hello");
        assertEquals("foobar0_0", invoker.invoke(service.get(), new Object[] { null }));
        assertEquals("foobar1_1", invoker.invoke(service.get(), new Object[] { null }));
        assertEquals("foobar2_2", invoker.invoke(service.get(), new Object[] { null }));

        assertEquals(3, MyDependency.CREATED);
        assertEquals(3, MyDependency.DESTROYED);

        assertEquals(3, MyTransitiveDependency.CREATED);
        assertEquals(3, MyTransitiveDependency.DESTROYED);
    }

    @Singleton
    static class MyService {
        public String hello(MyDependency dependency) {
            return "foobar" + dependency;
        }
    }

    @Dependent
    static class MyDependency {
        static int CREATED = 0;

        static int DESTROYED = 0;

        @Inject
        MyTransitiveDependency transitiveDependency;

        private int id;

        @PostConstruct
        public void init() {
            this.id = CREATED++;
        }

        @PreDestroy
        public void destroy() {
            DESTROYED++;
        }

        @Override
        public String toString() {
            return id + "_" + transitiveDependency;
        }
    }

    @Dependent
    static class MyTransitiveDependency {
        static int CREATED = 0;

        static int DESTROYED = 0;

        private int id;

        @PostConstruct
        public void init() {
            this.id = CREATED++;
        }

        @PreDestroy
        public void destroy() {
            DESTROYED++;
        }

        @Override
        public String toString() {
            return "" + id;
        }
    }
}
