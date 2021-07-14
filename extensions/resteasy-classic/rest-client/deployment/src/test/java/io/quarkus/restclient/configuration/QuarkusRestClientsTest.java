package io.quarkus.restclient.configuration;

import javax.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests clients configured with Quarkus-style configuration.
 */
public class QuarkusRestClientsTest extends MPRestClientsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoResource.class,
                            EchoClient.class, EchoClientWithConfigKey.class, ShortNameEchoClient.class))
            .withConfigurationResource("quarkus-restclients-test-application.properties");

    @RestClient
    ShortNameEchoClient shortNameClient;

    // tests for configKey and fully qualified clients inherited

    @Test
    public void shortNameClientShouldConnect() {
        Assertions.assertEquals("Hello", shortNameClient.echo("Hello"));
    }

    @Test
    void shortNameClientShouldHaveSingletonScope() {
        verifyClientScope(ShortNameEchoClient.class, Singleton.class);
    }

}
