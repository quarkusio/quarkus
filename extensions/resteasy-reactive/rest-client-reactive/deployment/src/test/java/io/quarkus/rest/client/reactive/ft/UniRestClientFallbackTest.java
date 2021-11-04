package io.quarkus.rest.client.reactive.ft;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class UniRestClientFallbackTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestEndpoint.class, Client.class, MyFallback.class)
                    .addAsResource(new StringAsset(setUrlForClass(Client.class)), "application.properties"));

    @Inject
    @RestClient
    Client client;

    @Test
    public void testFallbackWasUsed() {
        assertEquals("pong", client.ping().await().indefinitely());
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
        @Fallback(MyFallback.class)
        Uni<String> ping();
    }

    public static class MyFallback implements FallbackHandler<Uni<String>> {
        @Override
        public Uni<String> handle(ExecutionContext context) {
            return Uni.createFrom().item("pong");
        }
    }
}
