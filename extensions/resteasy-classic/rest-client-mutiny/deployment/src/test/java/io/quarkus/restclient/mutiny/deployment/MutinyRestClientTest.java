package io.quarkus.restclient.mutiny.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class MutinyRestClientTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MutinyRestClientTest.class, Client.class, TestEndpoint.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testUni() throws InterruptedException, ExecutionException {
        Client client = RestClientBuilder.newBuilder().baseUrl(url).build(Client.class);
        assertEquals("OK", client.ping().await().indefinitely());
    }

    @RegisterRestClient
    public interface Client {

        @GET
        @Path("/test")
        Uni<String> ping();

    }

    @Path("/test")
    public static class TestEndpoint {

        @GET
        public String get() {
            return "OK";
        }

    }
}
