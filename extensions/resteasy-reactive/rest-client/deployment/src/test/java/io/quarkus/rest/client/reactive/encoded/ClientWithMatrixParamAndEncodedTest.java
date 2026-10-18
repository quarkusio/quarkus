package io.quarkus.rest.client.reactive.encoded;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.ext.web.RoutingContext;

public class ClientWithMatrixParamAndEncodedTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, ClientWithoutEncoded.class,
                    ClientWithEncoded.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void encodesMatrixParamByDefault() {
        ClientWithoutEncoded client = RestClientBuilder.newBuilder().baseUri(baseUri).build(ClientWithoutEncoded.class);
        // '=' must be percent-encoded in matrix values
        assertThat(client.call("a=b")).isEqualTo(";name=a%3Db");
    }

    @Test
    void respectsEncodedMatrixParam() {
        ClientWithEncoded client = RestClientBuilder.newBuilder().baseUri(baseUri).build(ClientWithEncoded.class);
        // With @Encoded, '=' is left unencoded
        assertThat(client.call("a=b")).isEqualTo(";name=a=b");
    }

    @Path("/matrix-encoded")
    public static class Resource {
        @GET
        public String call(RoutingContext context) {
            String path = context.request().path();
            int idx = path.indexOf(";name=");
            return idx < 0 ? path : path.substring(idx);
        }
    }

    @Path("/matrix-encoded")
    public interface ClientWithoutEncoded {
        @GET
        String call(@MatrixParam("name") String name);
    }

    @Path("/matrix-encoded")
    public interface ClientWithEncoded {
        @GET
        String call(@Encoded @MatrixParam("name") String name);
    }
}
