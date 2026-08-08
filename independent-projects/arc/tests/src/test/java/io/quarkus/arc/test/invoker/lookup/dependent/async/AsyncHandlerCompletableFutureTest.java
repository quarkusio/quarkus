package io.quarkus.arc.test.invoker.lookup.dependent.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class AsyncHandlerCompletableFutureTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyDependency.class, MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                for (String name : List.of("helloSync", "helloAsync", "helloThrow")) {
                    MethodInfo method = bean.getImplClazz().firstMethod(name);
                    invokers.put(name, factory.createInvoker(bean, method)
                            .withInstanceLookup()
                            .withArgumentLookup(0)
                            .build());
                }
            }))
            .build();

    @Test
    public void testSync() throws Exception {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, CompletableFuture<String>> invoker = helper.getInvoker("helloSync");

        assertEquals(0, MyDependency.destroyedCounter.get());

        CompletableFuture<String> result = invoker.invoke(null, new Object[] { null });

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertTrue(result.isDone());
        assertEquals("hello", result.getNow(null));
    }

    @Test
    public void testAsync() throws Exception {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, CompletableFuture<String>> invoker = helper.getInvoker("helloAsync");
        CompletableFuture<String> future = new CompletableFuture<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        CompletableFuture<String> result = invoker.invoke(null, new Object[] { null, future });

        assertEquals(0, MyDependency.destroyedCounter.get());
        assertFalse(result.isDone());

        future.complete("hello");

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertTrue(result.isDone());
        assertEquals("hello", result.getNow(null));
    }

    @Test
    public void testSyncThrow() {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, CompletableFuture<String>> invoker = helper.getInvoker("helloThrow");

        assertEquals(0, MyDependency.destroyedCounter.get());

        assertThrows(IllegalArgumentException.class, () -> {
            invoker.invoke(null, new Object[] { null });
        });

        assertEquals(1, MyDependency.destroyedCounter.get());
    }

    @Dependent
    static class MyDependency {
        static AtomicInteger destroyedCounter = new AtomicInteger(0);

        static void reset() {
            destroyedCounter.set(0);
        }

        @PreDestroy
        void destroy() {
            destroyedCounter.incrementAndGet();
        }
    }

    @ApplicationScoped
    static class MyService {
        CompletableFuture<String> helloSync(MyDependency bean) {
            return CompletableFuture.completedFuture("hello");
        }

        CompletableFuture<String> helloAsync(MyDependency bean, CompletableFuture<String> future) {
            CompletableFuture<String> result = new CompletableFuture<>();
            future.whenComplete((value, error) -> {
                if (error == null) {
                    result.complete(value);
                } else {
                    result.completeExceptionally(error);
                }
            });
            return result;
        }

        CompletableFuture<String> helloThrow(MyDependency bean) {
            throw new IllegalArgumentException("synchronous throw");
        }
    }
}
