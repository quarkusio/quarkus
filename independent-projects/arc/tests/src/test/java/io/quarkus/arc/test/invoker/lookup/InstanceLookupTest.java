package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class InstanceLookupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method).withInstanceLookup().build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<MyService, String> invoker = helper.getInvoker("hello");
        assertEquals("foobar0", invoker.invoke(null, null));
        assertEquals("foobar0", invoker.invoke(null, null));
        assertEquals("foobar0", invoker.invoke(null, null));
        assertEquals(0, MyService.DESTROYED);
    }

    @Singleton
    static class MyService {
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

        public String hello() {
            return "foobar" + id;
        }
    }
}
