package io.quarkus.restclient.configuration;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests clients configured with MicroProfile-style configuration.
 */
public class MPRestClientsTest extends AbstractRestClientsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class,
                            EchoClient.class, EchoClientWithConfigKey.class, ShortNameEchoClient.class))
            .withConfigurationResource("mp-restclients-test-application.properties");
}
