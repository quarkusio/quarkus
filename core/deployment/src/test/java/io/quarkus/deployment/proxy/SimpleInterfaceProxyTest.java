package io.quarkus.deployment.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.TestClassLoader;

public class SimpleInterfaceProxyTest {

    @Test
    public void testProxyCreation() throws InstantiationException, IllegalAccessException {
        FirstArgInvocationHandler invocationHandler = new FirstArgInvocationHandler();
        ProxyConfiguration<Object> proxyConfiguration = new ProxyConfiguration<>()
                .setSuperClass(Object.class)
                .setAnchorClass(SimpleInterface.class)
                .setProxyNameSuffix("$Proxy2")
                .setClassLoader(new TestClassLoader(SimpleClass.class.getClassLoader()))
                .addAdditionalInterface(SimpleInterface.class);
        SimpleInterface instance = (SimpleInterface) new ProxyFactory<>(proxyConfiguration).newInstance(invocationHandler);

        String result = instance.capitalize("in");
        assertThat(result).isEqualTo("in");

        instance.doNothing();

        assertThat(invocationHandler.invocationCount).isEqualTo(2);
    }

}
