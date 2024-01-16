package io.quarkus.restclient.registerclientheaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RegisterClientHeadersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, EchoClient.class, MyHeadersFactory.class)
                    .addAsResource(
                            new StringAsset("io.quarkus.restclient.registerclientheaders.EchoClient/mp-rest/url=${test.url}"),
                            "application.properties"));

    @RestClient
    EchoClient client;

    @Test
    public void testProvider() {
        assertEquals("ping:bar", client.echo("ping:"));
    }

}
