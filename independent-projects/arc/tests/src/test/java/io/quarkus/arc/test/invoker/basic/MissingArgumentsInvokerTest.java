package io.quarkus.arc.test.invoker.basic;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class MissingArgumentsInvokerTest {
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
    public void test() {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertThrows(NullPointerException.class, () -> {
            hello.invoke(service.get(), null);
        });
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            hello.invoke(service.get(), new Object[] {});
        });
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            hello.invoke(new MyService(), new Object[] { 1 });
        });

        Invoker<MyService, String> helloStatic = helper.getInvoker("helloStatic");
        assertThrows(NullPointerException.class, () -> {
            helloStatic.invoke(null, null);
        });
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            helloStatic.invoke(null, new Object[] { "" });
        });
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            hello.invoke(null, new Object[] {});
        });
    }

    @Singleton
    static class MyService {
        public String hello(int param1, String param2) {
            return "foobar_" + param1 + param2;
        }

        public static String helloStatic(String param1, int param2) {
            return "quux_" + param1 + param2;
        }
    }
}
