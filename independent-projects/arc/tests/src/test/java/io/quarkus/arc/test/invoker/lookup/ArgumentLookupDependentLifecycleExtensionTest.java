package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;
import io.quarkus.arc.test.util.Barrier;

public class ArgumentLookupDependentLifecycleExtensionTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyDependency.class, MyTransitiveDependency.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method)
                        .withArgumentLookup(1)
                        .build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<MyService, CompletionStage<String>> invoker = helper.getInvoker("hello");

        assertInvocation(invoker, 0);
        assertInvocation(invoker, 1);
        assertInvocation(invoker, 2);
    }

    private void assertInvocation(Invoker<MyService, CompletionStage<String>> invoker, int counter) throws Exception {
        MyService service = Arc.container().instance(MyService.class).get();

        assertEquals(counter, MyDependency.CREATED);
        assertEquals(counter, MyDependency.DESTROYED);

        assertEquals(counter, MyTransitiveDependency.CREATED);
        assertEquals(counter, MyTransitiveDependency.DESTROYED);

        Barrier barrier = new Barrier();
        CompletionStage<String> completionStage = invoker.invoke(service, new Object[] { barrier, null });

        assertEquals(counter + 1, MyDependency.CREATED);
        assertEquals(counter, MyDependency.DESTROYED);

        assertEquals(counter + 1, MyTransitiveDependency.CREATED);
        assertEquals(counter, MyTransitiveDependency.DESTROYED);

        barrier.open();

        String result = completionStage.toCompletableFuture().get();
        assertEquals("foobar" + counter + "_" + counter, result);

        assertEquals(counter + 1, MyDependency.CREATED);
        assertEquals(counter + 1, MyDependency.DESTROYED);

        assertEquals(counter + 1, MyTransitiveDependency.CREATED);
        assertEquals(counter + 1, MyTransitiveDependency.DESTROYED);
    }

    @Singleton
    static class MyService {
        public CompletionStage<String> hello(Barrier barrier, MyDependency dependency) {
            CompletableFuture<String> result = new CompletableFuture<>();
            new Thread(() -> {
                try {
                    barrier.await();
                    result.complete("foobar" + dependency);
                } catch (InterruptedException e) {
                    result.completeExceptionally(e);
                }
            }).start();
            return result;
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
