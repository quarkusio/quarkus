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

public class MultipleAsyncHandlersMatchTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(MyAsyncType1Handler.class)
            .asyncHandler(MyAsyncType2Handler.class)
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

        // _not_ async (multiple async handlers match), so destroyed synchronously
        assertEquals(1, MyDependency.destroyedCounter.get());
        assertFalse(MyService.futureCompleted);

        future.complete("hello");

        assertTrue(MyService.futureCompleted);
        assertEquals(1, MyDependency.destroyedCounter.get());
    }

    public interface MyAsyncType1 {
    }

    public interface MyAsyncType2 {
    }

    public static class MyAsyncType1Handler implements AsyncHandler.ParameterType<MyAsyncType1> {
        @Override
        public MyAsyncType1 transformArgument(MyAsyncType1 original, Runnable completion) {
            return original;
        }
    }

    public static class MyAsyncType2Handler implements AsyncHandler.ParameterType<MyAsyncType2> {
        @Override
        public MyAsyncType2 transformArgument(MyAsyncType2 original, Runnable completion) {
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

        void hello(MyDependency bean, MyAsyncType1 async1, MyAsyncType2 async2, CompletableFuture<String> future) {
            future.whenComplete((value, error) -> {
                futureCompleted = true;
            });
        }
    }
}
