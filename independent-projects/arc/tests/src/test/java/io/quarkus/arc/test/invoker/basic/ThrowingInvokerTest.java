package io.quarkus.arc.test.invoker.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class ThrowingInvokerTest {
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
        assertEquals("foobar", hello.invoke(service.get(), new Object[] { false }));
        assertThrows(IllegalArgumentException.class, () -> hello.invoke(service.get(), new Object[] { true }));
        assertEquals("foobar", hello.invoke(new MyService(), new Object[] { false }));
        assertThrows(IllegalArgumentException.class, () -> hello.invoke(new MyService(), new Object[] { true }));

        Invoker<MyService, String> helloStatic = helper.getInvoker("helloStatic");
        assertEquals("quux", helloStatic.invoke(null, new Object[] { false }));
        assertThrows(IllegalStateException.class, () -> helloStatic.invoke(null, new Object[] { true }));
    }

    @Singleton
    static class MyService {
        public String hello(boolean fail) {
            if (fail) {
                throw new IllegalArgumentException();
            }
            return "foobar";
        }

        public static String helloStatic(boolean fail) {
            if (fail) {
                throw new IllegalStateException();
            }
            return "quux";
        }
    }
}
