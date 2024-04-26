package io.quarkus.arc.test.invoker.basic;

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

public class ExcessArgumentsInvokerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo hello = bean.getImplClazz().firstMethod("hello");
                MethodInfo helloStatic = bean.getImplClazz().firstMethod("helloStatic");
                for (MethodInfo method : List.of(hello, helloStatic)) {
                    invokers.put(method.name(), factory.createInvoker(bean, method).build());
                }
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertEquals("foobar_a", hello.invoke(service.get(), new Object[] { "a", "ignored" }));
        assertEquals("foobar_b", hello.invoke(new MyService(), new Object[] { "b", 1, 2, 3 }));

        Invoker<MyService, String> helloStatic = helper.getInvoker("helloStatic");
        assertEquals("quux_c", helloStatic.invoke(null, new Object[] { "c", new Object() }));
        assertEquals("quux_d", helloStatic.invoke(null, new Object[] { "d", List.of(), Set.of() }));
    }

    @Singleton
    static class MyService {
        public String hello(String param) {
            return "foobar_" + param;
        }

        public static String helloStatic(String param) {
            return "quux_" + param;
        }
    }
}
