package io.quarkus.rest.client.reactive;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static io.quarkus.rest.client.reactive.TestRestClientListener.HEADER_PARAM_NAME;
import static io.quarkus.rest.client.reactive.TestRestClientListener.HEADER_PARAM_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class RestClientListenerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class, TestRestClientListener.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(Client.class)),
                            "application.properties")
                    .addAsResource(
                            new StringAsset(TestRestClientListener.class.getCanonicalName()),
                            "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientListener"));

    @RestClient
    Client client;

    @Test
    void shouldCallRegisteredRestClient() {
        String result = client.get()
                .await().atMost(Duration.ofSeconds(10));
        assertThat(result).isEqualTo(HEADER_PARAM_VALUE);
    }

    @Path("/")
    @RegisterRestClient
    @Produces(MediaType.TEXT_PLAIN)
    interface Client {
        @GET
        Uni<String> get();
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    static class Resource {

        @GET
        public String get(@HeaderParam(HEADER_PARAM_NAME) String headerParam) {
            return headerParam;
        }
    }
}
