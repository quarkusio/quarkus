package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ClientWithWrongScopeTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyClient.class))
            .withConfigurationResource("wrong-scope-test-application.properties")
            .assertException(t -> assertThat(t).hasMessageContaining("Not possible to define the scope"));

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
