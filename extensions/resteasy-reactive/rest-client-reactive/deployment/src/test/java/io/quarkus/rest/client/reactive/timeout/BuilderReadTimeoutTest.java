package io.quarkus.rest.client.reactive.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class BuilderReadTimeoutTest {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Client.class, Resource.class));

    @Test
    void shouldTimeoutIfReadTimeoutSetShort() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .readTimeout(1, TimeUnit.SECONDS)
                .build(Client.class);

        RuntimeException exception = assertThrows(RuntimeException.class, client::slow);
        assertThat(exception).hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldNotTimeoutOnFastResponse() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .readTimeout(1, TimeUnit.SECONDS)
                .build(Client.class);

        assertThat(client.fast()).isEqualTo("fast-response");
    }

    @Test
    void shouldNotTimeoutOnDefaultTimeout() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);

        assertThat(client.slow()).isEqualTo("slow-response");
    }

    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static interface Client {
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
