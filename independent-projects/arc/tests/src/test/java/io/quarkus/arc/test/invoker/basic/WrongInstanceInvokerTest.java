package io.quarkus.arc.test.invoker.basic;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class WrongInstanceInvokerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method).build());
            }))
            .build();

    @Test
    public void test() {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<Object, Object> hello = helper.getInvoker("hello");
        assertThrows(ClassCastException.class, () -> {
            hello.invoke(new Object(), new Object[] { "" });
        });
        assertThrows(ClassCastException.class, () -> {
            hello.invoke(List.of(""), new Object[] { "" });
        });
    }

    @Singleton
    static class MyService {
        public String hello(String param) {
            return "foobar_" + param;
        }
    }
}
