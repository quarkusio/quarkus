package io.quarkus.arc.test.invoker.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

public class InstanceMethodInvokerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo hello = bean.getImplClazz().firstMethod("hello");
                MethodInfo doSomething = bean.getImplClazz().firstMethod("doSomething");
                MethodInfo fail = bean.getImplClazz().firstMethod("fail");
                for (MethodInfo method : List.of(hello, doSomething, fail)) {
                    invokers.put(method.name(), factory.createInvoker(bean, method).build());
                }
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertEquals("foobar0[]", hello.invoke(service.get(), new Object[] { 0, List.of() }));
        assertEquals("foobar1[]", hello.invoke(new MyService(), new Object[] { 1, List.of() }));
        assertThrows(NullPointerException.class, () -> {
            hello.invoke(null, new Object[] { 2, List.of() });
        });

        Invoker<Object, Object> helloDetyped = (Invoker) hello;
        assertEquals("foobar3[]", helloDetyped.invoke(service.get(), new Object[] { 3, List.of() }));
        assertEquals("foobar4[]", helloDetyped.invoke(new MyService(), new Object[] { 4, List.of() }));
        assertThrows(NullPointerException.class, () -> {
            helloDetyped.invoke(null, new Object[] { 5, List.of() });
        });

        Invoker<MyService, Void> doSomething = helper.getInvoker("doSomething");
        assertEquals(0, MyService.counter);
        assertNull(doSomething.invoke(service.get(), null));
        assertEquals(1, MyService.counter);
        assertNull(doSomething.invoke(new MyService(), new Object[] {}));
        assertEquals(2, MyService.counter);

        Invoker<MyService, Void> fail = helper.getInvoker("fail");
        assertNull(fail.invoke(service.get(), new Object[] { false }));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            fail.invoke(service.get(), new Object[] { true });
        });
        assertEquals("expected", ex.getMessage());
    }

    @Singleton
    static class MyService {
        public static int counter = 0;

        public String hello(int param1, List<String> param2) {
            return "foobar" + param1 + param2;
        }

        public void doSomething() {
            counter++;
        }

        public void fail(boolean doFail) {
            if (doFail) {
                throw new IllegalArgumentException("expected");
            }
        }
    }
}
