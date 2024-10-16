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

public class PrimitiveParametersInvokerTest {
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
        assertEquals("foobar_true_a_1", hello.invoke(service.get(), new Object[] { true, 'a', 1 }));
        assertEquals("foobar_false_b_2", hello.invoke(new MyService(), new Object[] { false, 'b', 2 }));
        assertThrows(NullPointerException.class, () -> {
            hello.invoke(new MyService(), new Object[] { null, null, null });
        });
        assertThrows(ClassCastException.class, () -> {
            hello.invoke(service.get(), new Object[] { true, 'a', 1L });
        });
        assertThrows(ClassCastException.class, () -> {
            hello.invoke(service.get(), new Object[] { true, 'a', 1.0 });
        });

        Invoker<MyService, String> helloStatic = helper.getInvoker("helloStatic");
        assertEquals("quux_1_1.0", helloStatic.invoke(null, new Object[] { 1L, 1.0 }));
        assertEquals("quux_1_1.0", helloStatic.invoke(null, new Object[] { 1, 1.0 }));
        assertEquals("quux_1_1.0", helloStatic.invoke(null, new Object[] { 1L, 1.0F }));
        assertThrows(NullPointerException.class, () -> {
            helloStatic.invoke(null, new Object[] { null, null });
        });
        assertThrows(ClassCastException.class, () -> {
            helloStatic.invoke(null, new Object[] { 1.0, 1.0 });
        });
    }

    @Singleton
    static class MyService {
        public String hello(boolean b, char c, int i) {
            return "foobar_" + b + "_" + c + "_" + i;
        }

        public static String helloStatic(long l, double d) {
            return "quux_" + l + "_" + d;
        }
    }
}
