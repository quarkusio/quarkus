package io.quarkus.rest.client.reactive;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.test.QuarkusUnitTest;

public class ContextualErrorMessagesTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, HelloClient2.class)
                    .addAsResource(new StringAsset(
                            "quarkus.rest-client.hello2.url=http://localhost:${quarkus.http.test-port:8081}/wrong-url"),
                            "application.properties"));

    @RestClient
    HelloClient2 client;

    /**
     * By default, contextual messages should be enabled.
     */
    @Test
    void errorMessageContainsContext() {
        try {
            client.echo("Bob");
            Assertions.fail("An exception was expected.");
        } catch (ClientWebApplicationException e) {
            Assertions.assertThat(e.getMessage()).contains("io.quarkus.rest.client.reactive.HelloClient2#echo");
        }
    }

}
