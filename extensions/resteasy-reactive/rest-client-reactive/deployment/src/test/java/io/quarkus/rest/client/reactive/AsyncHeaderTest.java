package io.quarkus.rest.client.reactive;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class AsyncHeaderTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(Client.class)),
                            "application.properties"));

    @RestClient
    Client client;

    @Test
    void shouldSendHeaderWithUni() {
        String headerValue = "jaka piekna i dluga wartosc headera";
        String result = client.uniGet(headerValue)
                .await().atMost(Duration.ofSeconds(10));
        assertThat(result).isEqualTo(String.format("passedHeader:%s", headerValue));
    }

    @Test
    void shouldSendHeaderWithCompletionStage() throws ExecutionException, InterruptedException, TimeoutException {
        String headerValue = "jaka piekna i dluga wartosc headera";
        String result = client.completionStageGet(headerValue)
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(String.format("passedHeader:%s", headerValue));
    }

    @Path("/")
    @RegisterRestClient
    @Produces(MediaType.TEXT_PLAIN)
    interface Client {
        @GET
        Uni<String> uniGet(@HeaderParam("some-header") String headerValue);

        @GET
        CompletionStage<String> completionStageGet(@HeaderParam("some-header") String headerValue);
    }

    @Path("/")
    static class Resource {
        @GET
        public String get(@HeaderParam("some-header") String headerValue) {
            return String.format("passedHeader:%s", headerValue);
        }
    }
}
