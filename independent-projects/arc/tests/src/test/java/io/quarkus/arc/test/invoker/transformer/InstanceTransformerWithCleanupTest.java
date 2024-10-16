package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

public class InstanceTransformerWithCleanupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo hello = bean.getImplClazz().firstMethod("hello");
                MethodInfo doSomething = bean.getImplClazz().firstMethod("doSomething");
                for (MethodInfo method : List.of(hello, doSomething)) {
                    invokers.put(method.name(), factory.createInvoker(bean, method)
                            .withInstanceTransformer(InstanceTransformerWithCleanup.class, "change")
                            .build());
                }
            }))
            .build();

    static class InstanceTransformerWithCleanup {
        static final AtomicInteger COUNTER = new AtomicInteger(0);

        static MyService change(MyService instance, Consumer<Runnable> cleanup) {
            if (instance != null) {
                return instance;
            }
            cleanup.accept(COUNTER::incrementAndGet);
            return new MyService("special");
        }
    }

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertEquals("defaultfoobar0[]", hello.invoke(service.get(), new Object[] { 0, List.of() }));
        assertEquals("specialfoobar0[]", hello.invoke(null, new Object[] { 0, List.of() }));

        Invoker<MyService, Set<String>> doSomething = helper.getInvoker("doSomething");
        assertEquals(Set.of("default", "_", "quux"), doSomething.invoke(service.get(), new Object[] { "_" }));
        assertEquals(Set.of("special", "_", "quux"), doSomething.invoke(null, new Object[] { "_" }));

        assertEquals(2, InstanceTransformerWithCleanup.COUNTER.get());
    }

    @Singleton
    static class MyService {
        private final String id;

        public MyService() {
            this("default");
        }

        public MyService(String id) {
            this.id = id;
        }

        public String hello(int param1, List<String> param2) {
            return id + "foobar" + param1 + param2;
        }

        public Set<String> doSomething(String param) {
            return Set.of("quux", param, id);
        }
    }
}
