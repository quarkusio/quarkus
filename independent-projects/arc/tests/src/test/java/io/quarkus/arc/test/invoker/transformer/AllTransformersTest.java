package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
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

public class AllTransformersTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method)
                        .withInstanceTransformer(Transformers.class, "instance")
                        .withArgumentTransformer(0, Transformers.class, "argument")
                        .withReturnValueTransformer(Transformers.class, "returnValue")
                        .withExceptionTransformer(Transformers.class, "exception")
                        .build());
            }))
            .build();

    static class Transformers {
        static final AtomicInteger instanceFinished = new AtomicInteger(0);

        static MyService instance(MyService instance, Consumer<Runnable> finisher) {
            if (instance != null) {
                return instance;
            }
            finisher.accept(instanceFinished::incrementAndGet);
            return new MyService("special");
        }

        static String argument(String argument) {
            return argument.toUpperCase(Locale.ROOT);
        }

        static String returnValue(String returnValue) {
            return returnValue.repeat(2);
        }

        static String exception(Throwable exception) throws Throwable {
            if (exception instanceof IllegalArgumentException) {
                return exception.getMessage() + "-exception";
            }
            throw exception;
        }
    }

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");

        assertEquals("default-FOOBARdefault-FOOBAR", hello.invoke(service.get(), new Object[] { "foobar" }));
        assertEquals("special-FOOBARspecial-FOOBAR", hello.invoke(null, new Object[] { "foobar" }));
        assertEquals("default-exception", hello.invoke(service.get(), new Object[] { "ill_arg" }));
        assertEquals("special-exception", hello.invoke(null, new Object[] { "ill_arg" }));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> hello.invoke(service.get(), new Object[] { "ill_state" }));
        assertEquals("default", exception.getMessage());
        exception = assertThrows(IllegalStateException.class, () -> hello.invoke(null, new Object[] { "ill_state" }));
        assertEquals("special", exception.getMessage());

        assertEquals(3, Transformers.instanceFinished.get());
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

        public String hello(String param) {
            if ("ILL_ARG".equals(param)) {
                throw new IllegalArgumentException(id);
            } else if ("ILL_STATE".equals(param)) {
                throw new IllegalStateException(id);
            }
            return id + "-" + param;
        }
    }
}
