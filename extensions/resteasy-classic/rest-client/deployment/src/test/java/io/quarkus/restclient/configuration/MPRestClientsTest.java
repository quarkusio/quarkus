package io.quarkus.restclient.configuration;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests clients configured with MicroProfile-style configuration.
 */
public class MPRestClientsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoResource.class,
                            EchoClient.class, EchoClientWithConfigKey.class, ShortNameEchoClient.class))
            .withConfigurationResource("mp-restclients-test-application.properties");

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
