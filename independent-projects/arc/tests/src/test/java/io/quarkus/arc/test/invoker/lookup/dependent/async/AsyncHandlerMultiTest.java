package io.quarkus.arc.test.invoker.lookup.dependent.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
import io.smallrye.mutiny.Multi;

public class AsyncHandlerMultiTest {
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
        Invoker<MyService, Multi<String>> invoker = helper.getInvoker("helloSync");

        assertEquals(0, MyDependency.destroyedCounter.get());

        Multi<String> result = invoker.invoke(null, new Object[] { null });

        assertEquals(0, MyDependency.destroyedCounter.get());

        AtomicReference<String> value = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean done = new AtomicBoolean(false);

        result.subscribe().with(value::set, error::set, () -> done.set(true));

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertEquals("hello", value.get());
        assertNull(error.get());
        assertTrue(done.get());
    }

    @Test
    public void testAsync() throws Exception {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, Multi<String>> invoker = helper.getInvoker("helloAsync");
        CompletableFuture<String> future = new CompletableFuture<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        Multi<String> result = invoker.invoke(null, new Object[] { null, future });

        AtomicReference<String> value = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean done = new AtomicBoolean(false);

        result.subscribe().with(value::set, error::set, () -> done.set(true));

        assertEquals(0, MyDependency.destroyedCounter.get());
        assertNull(value.get());
        assertNull(error.get());
        assertFalse(done.get());

        future.complete("hello");

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertEquals("hello", value.get());
        assertNull(error.get());
        assertTrue(done.get());
    }

    @Test
    public void testAsyncCancel() throws Exception {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, Multi<String>> invoker = helper.getInvoker("helloAsync");
        CompletableFuture<String> future = new CompletableFuture<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        Multi<String> result = invoker.invoke(null, new Object[] { null, future });

        AtomicReference<String> value = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean done = new AtomicBoolean(false);

        AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        result.subscribe().with(subscriptionRef::set, value::set, error::set, () -> done.set(true));

        assertEquals(0, MyDependency.destroyedCounter.get());
        assertNull(value.get());
        assertNull(error.get());
        assertFalse(done.get());

        subscriptionRef.get().cancel();

        assertEquals(1, MyDependency.destroyedCounter.get());
        assertNull(value.get());
        assertNull(error.get());
        assertFalse(done.get());
    }

    @Test
    public void testSyncThrow() {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, Multi<String>> invoker = helper.getInvoker("helloThrow");

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
        Multi<String> helloSync(MyDependency bean) {
            return Multi.createFrom().item("hello");
        }

        Multi<String> helloAsync(MyDependency bean, CompletableFuture<String> future) {
            return Multi.createFrom().completionStage(future);
        }

        Multi<String> helloThrow(MyDependency bean) {
            throw new IllegalArgumentException("synchronous throw");
        }
    }
}
