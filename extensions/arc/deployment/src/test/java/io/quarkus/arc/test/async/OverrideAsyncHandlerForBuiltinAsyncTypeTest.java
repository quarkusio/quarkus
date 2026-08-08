package io.quarkus.arc.test.async;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticInjections;
import jakarta.enterprise.invoke.AsyncHandler;
import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class OverrideAsyncHandlerForBuiltinAsyncTypeTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAction.class, MyBean.class, MyCompletableFutureAsyncHandler.class,
                            MyExtension.class, MyBeanCreator.class)
                    .addAsServiceProvider(AsyncHandler.ReturnType.class, MyCompletableFutureAsyncHandler.class)
                    .addAsServiceProvider(BuildCompatibleExtension.class, MyExtension.class))
            .overrideConfigKey("quarkus.arc.async-handler.\"java.util.concurrent.CompletableFuture\"",
                    MyCompletableFutureAsyncHandler.class.getName());

    @Inject
    MyBean bean;

    @Test
    public void test() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(i, MyCompletableFutureAsyncHandler.counter.get());
            assertEquals(i, MyAction.created.get());
            assertEquals(i, MyAction.destroyed.get());

            assertEquals("Hello, world!", bean.doSomething().get());

            assertEquals(i + 1, MyCompletableFutureAsyncHandler.counter.get());
            assertEquals(i + 1, MyAction.created.get());
            assertEquals(i + 1, MyAction.destroyed.get());
        }
    }

    @Dependent
    public static class MyAction {
        static AtomicInteger created = new AtomicInteger(0);
        static AtomicInteger destroyed = new AtomicInteger(0);

        public CompletableFuture<String> hello() {
            return CompletableFuture.supplyAsync(() -> "Hello, world!");
        }

        @PostConstruct
        public void created() {
            created.incrementAndGet();
        }

        @PreDestroy
        public void destroyed() {
            destroyed.incrementAndGet();
        }
    }

    public static class MyBean {
        private final Invoker<MyAction, CompletableFuture<String>> invoker;

        MyBean(Invoker<MyAction, CompletableFuture<String>> invoker) {
            this.invoker = invoker;
        }

        public CompletableFuture<String> doSomething() throws Exception {
            return invoker.invoke(null, null);
        }
    }

    public static class MyCompletableFutureAsyncHandler<T> implements AsyncHandler.ReturnType<CompletableFuture<T>> {
        static final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public CompletableFuture<T> transform(CompletableFuture<T> original, Runnable completion) {
            CompletableFuture<T> result = new CompletableFuture<>();
            original.whenComplete((value, error) -> {
                completion.run();
                counter.incrementAndGet();

                if (error == null) {
                    result.complete(value);
                } else {
                    result.completeExceptionally(error);
                }
            });
            return result;
        }
    }

    public static class MyExtension implements BuildCompatibleExtension {
        private InvokerInfo hello;

        @Registration(types = MyAction.class)
        public void registration(BeanInfo bean, InvokerFactory invokers) {
            MethodInfo method = bean.declaringClass()
                    .methods()
                    .stream()
                    .filter(it -> it.name().equals("hello"))
                    .findAny()
                    .orElseThrow();
            hello = invokers.createInvoker(bean, method)
                    .withInstanceLookup()
                    .build();
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            syn.addBean(MyBean.class)
                    .type(MyBean.class)
                    .scope(Dependent.class)
                    .withParam("invoker", hello)
                    .createWith(MyBeanCreator.class);
        }
    }

    public static class MyBeanCreator implements SyntheticBeanCreator<MyBean> {
        public MyBean create(SyntheticInjections injections, Parameters params) {
            return new MyBean(params.get("invoker", Invoker.class));
        }
    }
}
