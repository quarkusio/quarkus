package io.quarkus.arc.test.invoker.lookup.dependent.async.invalid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class MultipleParametersMatchTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(MyAsyncTypeHandler.class)
            .beanClasses(MyDependency.class, MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                for (String name : List.of("hello")) {
                    MethodInfo method = bean.getImplClazz().firstMethod(name);
                    invokers.put(name, factory.createInvoker(bean, method)
                            .withInstanceLookup()
                            .withArgumentLookup(0)
                            .build());
                }
            }))
            .build();

    @Test
    public void test() throws Exception {
        MyDependency.reset();

        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        Invoker<MyService, Void> invoker = helper.getInvoker("hello");
        CompletableFuture<String> future = new CompletableFuture<>();

        assertEquals(0, MyDependency.destroyedCounter.get());

        invoker.invoke(null, new Object[] { null, null, null, future });

        // _not_ async (multiple matching parameters), so destroyed synchronously
        assertEquals(1, MyDependency.destroyedCounter.get());
        assertFalse(MyService.futureCompleted);

        future.complete("hello");

        assertTrue(MyService.futureCompleted);
        assertEquals(1, MyDependency.destroyedCounter.get());
    }

    public interface MyAsyncType {
    }

    public static class MyAsyncTypeHandler implements AsyncHandler.ParameterType<MyAsyncType> {
        @Override
        public MyAsyncType transformArgument(MyAsyncType original, Runnable completion) {
            return original;
        }
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
        static boolean futureCompleted = false;

        void hello(MyDependency bean, MyAsyncType async1, MyAsyncType async2, CompletableFuture<String> future) {
            future.whenComplete((value, error) -> {
                futureCompleted = true;
            });
        }
    }
}
