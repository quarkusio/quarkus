package io.quarkus.it.resteasy.rest.client.reactive;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Assumptions;

import java.net.URI;

@QuarkusIntegrationTest
class GreetEndpointIT extends GreetEndpointTest {
    GreetEndpointIT() {
        super(buildRestClient());
    }

    private static GreetClient buildRestClient() {
        Assumptions.abort(
            "FIXME: org.jboss.resteasy.reactive.client.impl.WebTargetImpl.proxy(WebTargetImpl.java:449) " +
            "java.lang.IllegalArgumentException: Not a REST client interface: " +
            "interface io.quarkus.it.resteasy.rest.client.reactive.GreetRestClient. " +
            "No @Path annotation found on the class or any methods of the interface " +
            "and no HTTP method annotations (@POST, @PUT, @GET, @HEAD, @DELETE, etc) " +
            "found on any of the methods"
        );
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(Constants.DEFAULT_TEST_URL))
                .build(GreetClient.class);
    }
}
