package io.quarkus.it.resteasy.rest.client.classic;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;

@QuarkusIntegrationTest
class GreetEndpointIT extends GreetEndpointTest {
    GreetEndpointIT() {
        super(buildRestClient());
    }

    private static GreetClient buildRestClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(Constants.DEFAULT_TEST_URL))
                .build(GreetClient.class);
    }
}
