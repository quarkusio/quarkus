package io.quarkus.rest.client.reactive.ft;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AsyncRestClientFallbackTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestEndpoint.class, Client.class, MyFallback.class)
                    .addAsResource(new StringAsset(setUrlForClass(Client.class)), "application.properties"));

    @Inject
    @RestClient
    Client client;

    @Test
    public void testFallbackWasUsed() throws ExecutionException, InterruptedException {
        assertEquals("pong", client.ping().toCompletableFuture().get());
    }

    @Path("/test")
    public static class TestEndpoint {
        @GET
        public String get() {
            throw new WebApplicationException(404);
        }
    }

    @RegisterRestClient
    public interface Client {
        @GET
        @Path("/test")
        @Asynchronous
        @Fallback(MyFallback.class)
        CompletionStage<String> ping();
    }

    public static class MyFallback implements FallbackHandler<CompletionStage<String>> {
        @Override
        public CompletionStage<String> handle(ExecutionContext context) {
            return completedFuture("pong");
        }
    }
}
