package io.quarkus.arc.test.invoker.lookup.dependent.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

public class AsyncHandlerCustomParamTypeTest {
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
        Invoker<MyService, Void> invoker = helper.getInvoker("helloSync");
        MyAsyncType<String> result = new MyAsyncType<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        invoker.invoke(null, new Object[] { null, result });

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertTrue(result.isComplete());
        assertEquals("hello", result.getIfComplete());
    }

    @Test
    public void testAsync() throws Exception {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, Void> invoker = helper.getInvoker("helloAsync");
        CompletableFuture<String> future = new CompletableFuture<>();
        MyAsyncType<String> result = new MyAsyncType<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        invoker.invoke(null, new Object[] { null, future, result });

        assertEquals(0, MyDependency.destroyedCounter.get());
        assertFalse(result.isComplete());

        future.complete("hello");

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertTrue(result.isComplete());
        assertEquals("hello", result.getIfComplete());
    }

    @Test
    public void testSyncThrow() {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, Void> invoker = helper.getInvoker("helloThrow");
        CompletableFuture<String> future = new CompletableFuture<>();
        MyAsyncType<String> result = new MyAsyncType<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        assertThrows(IllegalArgumentException.class, () -> {
            invoker.invoke(null, new Object[] { null, future, result });
        });

        assertEquals(1, MyDependency.destroyedCounter.get());

        // calls the completion callback, which must be a noop
        future.complete("hello");

        assertEquals(1, MyDependency.destroyedCounter.get());
    }

    public static class MyAsyncType<T> {
        private final AtomicReference<T> value = new AtomicReference<>(null);
        private final AtomicReference<Runnable> callback = new AtomicReference<>(null);

        public boolean isComplete() {
            return value.get() != null;
        }

        public T getIfComplete() {
            T value = this.value.get();
            if (value != null) {
                return value;
            } else {
                throw new IllegalStateException("not yet complete");
            }
        }

        public void whenComplete(Runnable callback) {
            if (!this.callback.compareAndSet(null, callback)) {
                throw new IllegalStateException("only one callback possible");
            }
        }

        public void resume(T value) {
            if (value == null) {
                throw new IllegalArgumentException("must resume with non-null value");
            }
            if (this.value.compareAndSet(null, value)) {
                Runnable callback = this.callback.get();
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    public static class MyAsyncTypeHandler<T> implements AsyncHandler.ParameterType<MyAsyncType<T>> {
        public MyAsyncTypeHandler() {
        }

        @Override
        public MyAsyncType<T> transformArgument(MyAsyncType<T> original, Runnable completion) {
            original.whenComplete(completion);
            return original;
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
        public void helloSync(MyDependency bean, MyAsyncType<String> async) {
            async.resume("hello");

            // completion was signaled, but the method didn't return yet,
            // so the dependency must not be destroyed
            assertEquals(0, MyDependency.destroyedCounter.get());
        }

        public void helloAsync(MyDependency bean, CompletableFuture<String> future, MyAsyncType<String> async) {
            future.whenComplete((value, error) -> {
                assertNull(error);
                async.resume(value);
            });
        }

        public void helloThrow(MyDependency bean, CompletableFuture<String> future, MyAsyncType<String> async) {
            future.whenComplete((value, error) -> {
                assertNull(error);
                async.resume(value);
            });
            throw new IllegalArgumentException("synchronous throw");
        }
    }
}
