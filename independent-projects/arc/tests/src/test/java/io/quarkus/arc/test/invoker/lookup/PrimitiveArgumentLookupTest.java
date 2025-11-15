package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class PrimitiveArgumentLookupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyProducers.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method)
                        .withArgumentLookup(0)
                        .withArgumentLookup(1)
                        .build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> invoker = helper.getInvoker("hello");
        assertEquals("foobar13_42", invoker.invoke(service.get(), new Object[] { null, null }));
    }

    @Singleton
    static class MyService {
        public String hello(int dependency1, long dependency2) {
            return "foobar" + dependency1 + "_" + dependency2;
        }
    }

    @Singleton
    static class MyProducers {
        @Produces
        public int dependency1() {
            return 13;
        }

        @Produces
        public Long dependency2() {
            return 42L;
        }
    }
}
