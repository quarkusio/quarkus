package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Inject;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class InstanceLookupDependentTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyDependency.class, MyTransitiveDependency.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method).withInstanceLookup().build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<MyService, String> invoker = helper.getInvoker("hello");
        assertEquals("foobar0_0_0", invoker.invoke(null, null));
        assertEquals("foobar1_1_1", invoker.invoke(null, null));
        assertEquals("foobar2_2_2", invoker.invoke(null, null));

        assertEquals(3, MyService.CREATED);
        assertEquals(3, MyDependency.CREATED);
        assertEquals(3, MyTransitiveDependency.CREATED);

        assertEquals(3, MyService.DESTROYED);
        assertEquals(3, MyDependency.DESTROYED);
        assertEquals(3, MyTransitiveDependency.DESTROYED);
    }

    @Dependent
    static class MyService {
        static int CREATED = 0;

        static int DESTROYED = 0;

        @Inject
        MyDependency dependency;

        private int id;

        @PostConstruct
        public void init() {
            this.id = CREATED++;
        }

        @PreDestroy
        public void destroy() {
            DESTROYED++;
        }

        public String hello() {
            return "foobar" + id + "_" + dependency;
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
