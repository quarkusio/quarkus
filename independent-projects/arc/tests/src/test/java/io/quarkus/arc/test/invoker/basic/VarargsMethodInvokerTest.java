package io.quarkus.arc.test.invoker.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
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

public class VarargsMethodInvokerTest {
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
        assertEquals("foobar0[]", hello.invoke(service.get(), new Object[] { 0, new String[] {} }));
        assertEquals("foobar1[a]", hello.invoke(new MyService(), new Object[] { 1, new String[] { "a" } }));

        Invoker<MyService, String> helloStatic = helper.getInvoker("helloStatic");
        assertEquals("quux0[b]", helloStatic.invoke(null, new Object[] { 0, new String[] { "b" } }));
        assertEquals("quux1[c]", helloStatic.invoke(null, new Object[] { 1, new String[] { "c" } }));
    }

    @Singleton
    static class MyService {
        public String hello(int param1, String... param2) {
            return "foobar" + param1 + Arrays.toString(param2);
        }

        public static String helloStatic(int param1, String... param2) {
            return "quux" + param1 + Arrays.toString(param2);
        }
    }
}
