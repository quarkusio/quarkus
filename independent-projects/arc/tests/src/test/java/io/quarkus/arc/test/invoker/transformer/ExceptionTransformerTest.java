package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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

public class ExceptionTransformerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo hello = bean.getImplClazz().firstMethod("hello");
                MethodInfo doSomething = bean.getImplClazz().firstMethod("doSomething");
                for (MethodInfo method : List.of(hello, doSomething)) {
                    invokers.put(method.name(), factory.createInvoker(bean, method)
                            .withExceptionTransformer(ExceptionTransformer.class, "change")
                            .build());
                }
            }))
            .build();

    static class ExceptionTransformer {
        static String change(Throwable exception) {
            if (exception instanceof IllegalArgumentException) {
                return "hello";
            } else if (exception instanceof IllegalStateException) {
                return "doSomething";
            } else {
                throw new AssertionError();
            }
        }
    }

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertEquals("hello", hello.invoke(service.get(), null));

        Invoker<MyService, String> doSomething = helper.getInvoker("doSomething");
        assertEquals("doSomething", doSomething.invoke(service.get(), null));
    }

    @Singleton
    static class MyService {
        public String hello() {
            throw new IllegalArgumentException();
        }

        public String doSomething() {
            throw new IllegalStateException();
        }
    }
}
