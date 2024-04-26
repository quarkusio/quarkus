package io.quarkus.arc.test.invoker.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class InvocationWrapperWithLookupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo hello = bean.getImplClazz().firstMethod("hello");
                MethodInfo doSomething = bean.getImplClazz().firstMethod("doSomething");
                for (MethodInfo method : List.of(hello, doSomething)) {
                    invokers.put(method.name(), factory.createInvoker(bean, method)
                            .withInstanceLookup()
                            .withInvocationWrapper(InvocationWrapper.class, "wrap")
                            .build());
                }
            }))
            .build();

    static class InvocationWrapper {
        static Object wrap(MyService instance, Object[] arguments, Invoker<MyService, Object> invoker) throws Exception {
            Object result = invoker.invoke(instance, arguments);
            if (result instanceof Set) {
                return ((Set<String>) result).stream().map(it -> it.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
            } else if (result instanceof String) {
                return ((String) result).toUpperCase(Locale.ROOT);
            } else {
                throw new AssertionError();
            }
        }
    }

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertEquals("FOOBAR0[]", hello.invoke(null, new Object[] { 0, List.of() }));

        Invoker<MyService, Set<String>> doSomething = helper.getInvoker("doSomething");
        assertEquals(Set.of("_", "QUUX"), doSomething.invoke(null, new Object[] { "_" }));
    }

    @Singleton
    static class MyService {
        public String hello(int param1, List<String> param2) {
            return "foobar" + param1 + param2;
        }

        public Set<String> doSomething(String param) {
            return Set.of("quux", param);
        }
    }
}
