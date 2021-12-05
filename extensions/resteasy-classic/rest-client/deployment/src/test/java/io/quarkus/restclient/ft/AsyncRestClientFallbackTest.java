package io.quarkus.restclient.ft;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class AsyncRestClientFallbackTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AsyncRestClientFallbackTest.class, Client.class, MyFallback.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testFallbackWasUsed() throws ExecutionException, InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUrl(url).build(Client.class);
        assertEquals("pong", client.ping().toCompletableFuture().get());
    }

    @RegisterRestClient
    public interface Client {

        @Asynchronous
        @Fallback(MyFallback.class)
        @GET
        @Path("/test")
        CompletionStage<String> ping();

    }

    @Path("/test")
    public static class TestEndpoint {

        @GET
        String get() {
            throw new WebApplicationException(404);
        }

    }

    public static class MyFallback implements FallbackHandler<CompletionStage<String>> {

        @Override
        public CompletionStage<String> handle(ExecutionContext context) {
            return completedFuture("pong");
        }

    }
}
