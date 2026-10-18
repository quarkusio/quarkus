package io.quarkus.deployment.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.TestClassLoader;

public class SimpleClassProxyTest {

    @Test
    public void testProxyCreation() throws InstantiationException, IllegalAccessException {
        SimpleInvocationHandler invocationHandler = new SimpleInvocationHandler();
        ProxyConfiguration<SimpleClass> proxyConfiguration = new ProxyConfiguration<SimpleClass>()
                .setSuperClass(SimpleClass.class)
                .setAnchorClass(SimpleClass.class)
                .setProxyNameSuffix("$$Proxy1")
                .setClassLoader(new TestClassLoader(SimpleClass.class.getClassLoader()));
        SimpleClass instance = new ProxyFactory<>(proxyConfiguration).newInstance(invocationHandler);

        assertMethod1(instance);
        assertMethod2(instance);
        assertMethod3(instance);
        assertMethod4(instance);

        assertThat(invocationHandler.invocationCount).isEqualTo(4);
    }

    private void assertMethod1(SimpleClass instance) {
        Object result = instance.method1();
        assertThat(result.getClass().isArray()).isTrue();
        assertThat((Object[]) result).isEmpty();
    }

    private void assertMethod2(SimpleClass instance) {
        Object result = instance.method2(10, 0, this, new int[0]);
        assertThat(result.getClass().isArray()).isTrue();
        assertThat((Object[]) result).hasSize(4).contains(10L, 0.0);
    }

    private void assertMethod3(SimpleClass instance) {
        Object result = instance.method3(Arrays.asList(1, 2, 3));
        assertThat(result.getClass().isArray()).isTrue();
        assertThat((Object[]) result).hasSize(1).satisfies(o -> {
            assertThat(o[0]).isInstanceOfSatisfying(List.class, l -> {
                assertThat(l).containsExactly(1, 2, 3);
            });
        });
    }

    private void assertMethod4(SimpleClass instance) {
        assertThatCode(() -> instance.method4(4)).doesNotThrowAnyException();
    }

}
