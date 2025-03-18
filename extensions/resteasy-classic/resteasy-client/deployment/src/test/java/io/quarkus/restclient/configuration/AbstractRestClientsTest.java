package io.quarkus.restclient.configuration;

import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;

/**
 * Tests clients configured with MicroProfile-style configuration.
 */
abstract class AbstractRestClientsTest {

    @RestClient
    EchoClientWithConfigKey clientWithConfigKey;

    @RestClient
    EchoClient fullClassNameClient;

    @Test
    public void clientWithConfigKeyShouldConnect() {
        Assertions.assertEquals("Hello", clientWithConfigKey.echo("Hello"));
    }

    @Test
    void clientWithConfigShouldHaveSingletonScope() {
        verifyClientScope(EchoClientWithConfigKey.class, Singleton.class);
    }

    @Test
    public void fullClassNameClientShouldConnect() {
        Assertions.assertEquals("Hello", fullClassNameClient.echo("Hello"));
    }

    @Test
    void fullClassNameClientShouldHaveSingletonScope() {
        verifyClientScope(EchoClient.class, Singleton.class);
    }

    static void verifyClientScope(Class clientInterface, Class expectedScope) {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(clientInterface, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        Assertions.assertEquals(expectedScope, resolvedBean.getScope());
    }

}
