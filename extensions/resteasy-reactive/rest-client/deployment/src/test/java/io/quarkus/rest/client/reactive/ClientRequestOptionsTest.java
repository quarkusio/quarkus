package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.api.ClientRequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ClientRequestOptionsTest {

    @RestClient
    Client client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.client.read-timeout", "1000")
            .overrideRuntimeConfigKey("quarkus.rest-client.client.url",
                    "http://${quarkus.http.host}:${quarkus.http.test-port}");

    @Test
    void shouldUseReadTimeoutOfRequestOptions() {
        ClientRequestOptions options = ClientRequestOptions.builder().readTimeout(10, TimeUnit.SECONDS).build();
        assertThat(client.slow(options)).isEqualTo("slow-response");
    }

    @Test
    void shouldUseReadTimeoutOfClientWithoutRequestOptions() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> client.slow(null));
        assertThat(exception).hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldFollowRedirectsOfRequestOptions() {
        assertThat(client.redirect(null).getStatus()).isEqualTo(302);

        ClientRequestOptions options = ClientRequestOptions.builder().followRedirects(true).build();
        assertThat(client.redirect(options).getStatus()).isEqualTo(200);
    }

    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @RegisterRestClient(configKey = "client")
    public interface Client {
        @GET
        @Path("/slow")
        String slow(ClientRequestOptions options);

        @GET
        @Path("/redirect")
        Response redirect(ClientRequestOptions options);
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("/slow")
        public String slow() throws InterruptedException {
            Thread.sleep(3000L);
            return "slow-response";
        }

        @GET
        @Path("/redirect")
        public Response redirect() {
            return Response.status(Response.Status.FOUND).location(URI.create("/fast")).build();
        }

        @GET
        @Path("/fast")
        public String fast() {
            return "fast-response";
        }
    }
}
