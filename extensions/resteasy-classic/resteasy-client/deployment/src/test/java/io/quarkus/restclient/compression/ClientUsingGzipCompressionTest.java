package io.quarkus.restclient.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.GZIP;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ClientUsingGzipCompressionTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyResource.class, MyClient.class))
            .withConfigurationResource("client-using-gzip-application.properties");

    @RestClient
    MyClient client;

    /**
     * Test that covers the property `quarkus.resteasy.gzip.max-input`.
     * Larger payloads than 10 bytes should return HTTP Request Too Large.
     */
    @Test
    public void testGzipMaxInput() {
        WebApplicationException ex = Assertions.assertThrows(WebApplicationException.class, () -> client.gzip(new byte[11]));
        assertEquals(HttpStatus.SC_REQUEST_TOO_LONG, ex.getResponse().getStatus());

        // verify shorter message works fine
        Assertions.assertEquals("Worked!", client.gzip(new byte[10]));
    }

    @Path("/client")
    @RegisterRestClient(configKey = "my-client")
    public interface MyClient {

        @POST
        @Path("/gzip")
        String gzip(@GZIP byte[] message);

    }

    @Path("/client")
    public class MyResource {

        @POST
        @Path("/gzip")
        public String gzip(@GZIP String message) {
            return "Worked!";
        }

    }
}
