package io.quarkus.restclient.ft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ExpectLogMessage;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientFallbackTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RestClientFallbackTest.class, Client.class, MyFallback.class));

    @TestHTTPResource
    URL url;

    @Test
    @ExpectLogMessage("Not Found")
    public void testFallbackWasUsed() throws Exception {
        try (Client client = RestClientBuilder.newBuilder().baseUrl(url).build(Client.class)) {
            assertEquals("pong", client.ping());
        }
    }

    @RegisterRestClient
    public interface Client extends AutoCloseable {

        @Fallback(MyFallback.class)
        @GET
        @Path("/test")
        String ping();

    }

    @Path("/test")
    public static class TestEndpoint {

        @GET
        public String get() {
            throw new WebApplicationException(404);
        }

    }

    public static class MyFallback implements FallbackHandler<String> {

        @Override
        public String handle(ExecutionContext context) {
            return "pong";
        }

    }
}
