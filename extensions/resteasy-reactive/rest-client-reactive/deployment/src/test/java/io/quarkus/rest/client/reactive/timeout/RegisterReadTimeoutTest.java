package io.quarkus.rest.client.reactive.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RegisterReadTimeoutTest {

    @RestClient
    Client client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class)
                    .addAsResource(new StringAsset(
                            "client/mp-rest/readTimeout=1000\nclient/mp-rest/url=http://${quarkus.http.host}:${quarkus.http.test-port}"),
                            "application.properties"));

    @Test
    void shouldTimeoutIfReadTimeoutSetShort() {
        RuntimeException exception = assertThrows(RuntimeException.class, client::slow);
        assertThat(exception).hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldNotTimeoutOnFastResponse() {
        assertThat(client.fast()).isEqualTo("fast-response");
    }

    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @RegisterRestClient(configKey = "client")
    public interface Client {
        @GET
        @Path("/slow")
        String slow();

        @GET
        @Path("/fast")
        String fast();
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static class Resource {

        @Path("/slow")
        @GET
        public String slow() throws InterruptedException {
            Thread.sleep(5000L);
            return "slow-response";
        }

        @Path("/fast")
        @GET
        public CompletionStage<String> fast() {
            return CompletableFuture.completedFuture("fast-response");
        }

    }
}
