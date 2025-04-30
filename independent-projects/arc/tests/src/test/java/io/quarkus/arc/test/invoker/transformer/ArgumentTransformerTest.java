package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

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

public class ArgumentTransformerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo hello = bean.getImplClazz().firstMethod("hello");
                MethodInfo doSomething = bean.getImplClazz().firstMethod("doSomething");
                MethodInfo increment = bean.getImplClazz().firstMethod("increment");
                for (MethodInfo method : List.of(hello, doSomething, increment)) {
                    invokers.put(method.name(), factory.createInvoker(bean, method)
                            .withArgumentTransformer(0, ArgumentTransformer.class, "change")
                            .build());
                }
            }))
            .build();

    static class ArgumentTransformer {
        static int change(int argument) {
            return argument + 1;
        }

        static String change(String argument) {
            return argument.repeat(2);
        }

        static double change(MyClass argument) {
            return argument.value;
        }
    }

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertEquals("foobar1[]", hello.invoke(service.get(), new Object[] { 0, List.of() }));

        Invoker<MyService, Set<String>> doSomething = helper.getInvoker("doSomething");
        assertEquals(Set.of("__", "quux"), doSomething.invoke(service.get(), new Object[] { "_" }));

        Invoker<MyService, Long> increment = helper.getInvoker("increment");
        assertEquals(3L, increment.invoke(service.get(), new Object[] { new MyClass(2.0) }));
    }

    @Singleton
    static class MyService {
        public String hello(int param1, List<String> param2) {
            return "foobar" + param1 + param2;
        }

        public Set<String> doSomething(String param) {
            return Set.of("quux", param);
        }

        public long increment(double param) {
            return (long) param + 1;
        }
    }

    static class MyClass {
        double value;

        MyClass(double value) {
            this.value = value;
        }
    }
}
