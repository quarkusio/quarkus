package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ClientWithWrongScopeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyClient.class))
            .withConfigurationResource("client-with-wrong-scope.properties")
            .assertException(t -> assertThat(t).hasMessageContaining("Not possible to define the scope"));

    @RestClient
    MyClient client;

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        fail();
    }

    @Path("/client")
    @RegisterRestClient(configKey = "my-client")
    public interface MyClient {

        @GET
        @Path("/")
        String get();
    }
}
