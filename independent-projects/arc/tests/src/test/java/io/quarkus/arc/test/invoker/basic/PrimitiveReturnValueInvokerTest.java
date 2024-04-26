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

public class PrimitiveReturnValueInvokerTest {
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

        Invoker<MyService, Integer> hello = helper.getInvoker("hello");
        assertEquals(3, hello.invoke(service.get(), new Object[] { 2 }));
        assertEquals(5, hello.invoke(new MyService(), new Object[] { 4 }));
        assertThrows(NullPointerException.class, () -> {
            hello.invoke(new MyService(), new Object[] { null });
        });
        assertThrows(ClassCastException.class, () -> {
            hello.invoke(service.get(), new Object[] { 1L });
        });
        assertThrows(ClassCastException.class, () -> {
            hello.invoke(service.get(), new Object[] { 1.0 });
        });

        Invoker<MyService, Double> helloStatic = helper.getInvoker("helloStatic");
        assertEquals(3.0, helloStatic.invoke(null, new Object[] { 2.0 }));
        assertEquals(5.0, helloStatic.invoke(null, new Object[] { 4.0F }));
        assertThrows(NullPointerException.class, () -> {
            helloStatic.invoke(null, new Object[] { null });
        });
    }

    @Singleton
    static class MyService {
        public int hello(int i) {
            return 1 + i;
        }

        public static double helloStatic(double d) {
            return 1.0 + d;
        }
    }
}
