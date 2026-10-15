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
import jakarta.enterprise.invoke.AsyncHandler;
import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;
import io.quarkus.arc.test.invoker.lookup.dependent.async.invalid.MyAsyncType;

public class AsyncHandlerCustomReturnTypeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(MyAsyncTypeHandler.class)
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
        Invoker<MyService, MyAsyncType<String>> invoker = helper.getInvoker("helloSync");

        assertEquals(0, MyDependency.destroyedCounter.get());

        MyAsyncType<String> result = invoker.invoke(null, new Object[] { null });

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertTrue(result.isComplete());
        assertEquals("hello", result.getIfComplete());
    }

    @Test
    public void testAsync() throws Exception {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, MyAsyncType<String>> invoker = helper.getInvoker("helloAsync");
        CompletableFuture<String> future = new CompletableFuture<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        MyAsyncType<String> result = invoker.invoke(null, new Object[] { null, future });

        assertEquals(0, MyDependency.destroyedCounter.get());
        assertFalse(result.isComplete());

        future.complete("hello");

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertTrue(result.isComplete());
        assertEquals("hello", result.getIfComplete());
    }

    // this test doesn't really verify anything, because an async handler for return type runs _after_ the method
    // is invoked, and if the invoked method throws an exception synchronously, there's no object the async handler
    // could transform
    @Test
    public void testSyncThrow() {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, MyAsyncType<String>> invoker = helper.getInvoker("helloThrow");
        CompletableFuture<String> future = new CompletableFuture<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        assertThrows(IllegalArgumentException.class, () -> {
            invoker.invoke(null, new Object[] { null, future });
        });

        assertEquals(1, MyDependency.destroyedCounter.get());

        // doesn't do anything
        future.complete("hello");

        assertEquals(1, MyDependency.destroyedCounter.get());
    }

    public static class MyAsyncType<T> {
        private final CompletableFuture<T> future;

        MyAsyncType(CompletableFuture<T> future) {
            this.future = future;
        }

        public boolean isComplete() {
            return future.isDone();
        }

        public T getIfComplete() {
            if (future.isDone()) {
                return future.getNow(null);
            } else {
                throw new IllegalStateException("not yet complete");
            }
        }

        public MyAsyncType<T> whenComplete(Runnable callback) {
            CompletableFuture<T> newFuture = new CompletableFuture<>();
            future.whenComplete((value, error) -> {
                callback.run();

                if (error == null) {
                    newFuture.complete(value);
                } else {
                    newFuture.completeExceptionally(error);
                }
            });
            return new MyAsyncType<>(newFuture);
        }
    }

    public static class MyAsyncTypeHandler<T> implements AsyncHandler.ReturnType<MyAsyncType<T>> {
        public MyAsyncTypeHandler() {
        }

        @Override
        public MyAsyncType<T> transform(MyAsyncType<T> original, Runnable completion) {
            return original.whenComplete(completion);
        }
    }

    @Dependent
    static class MyDependency {
        public static AtomicInteger destroyedCounter = new AtomicInteger(0);

        public static void reset() {
            destroyedCounter.set(0);
        }

        @PreDestroy
        public void destroy() {
            destroyedCounter.incrementAndGet();
        }
    }

    @ApplicationScoped
    static class MyService {
        MyAsyncType<String> helloSync(MyDependency bean) {
            return new MyAsyncType<>(CompletableFuture.completedFuture("hello"));
        }

        MyAsyncType<String> helloAsync(MyDependency bean, CompletableFuture<String> future) {
            return new MyAsyncType<>(future);
        }

        MyAsyncType<String> helloThrow(MyDependency bean, CompletableFuture<String> future) {
            MyAsyncType<String> ignored = new MyAsyncType<>(future);
            throw new IllegalArgumentException("synchronous throw");
        }
    }
}
