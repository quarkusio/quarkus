package io.quarkus.restclient.configuration;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class RestClientProfilesConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class,
                            EchoClient.class, EchoClientWithConfigKey.class))
            .withConfigurationResource("profiles-application.properties");

    @RestClient
    EchoClient echoClient;

    @RestClient
    EchoClientWithConfigKey shortNameClient;

    @Test
    public void shouldRespond() {
        Assertions.assertEquals("Hello", echoClient.echo("Hello"));
        Assertions.assertEquals("Hello", shortNameClient.echo("Hello"));
    }

}
