package io.quarkus.rest.client.reactive.encoded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientWithPathParamAndEncodedTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    @Test
    public void testClientWithoutEncoded() {
        ClientWithoutEncoded client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithoutEncoded.class);
        ClientWebApplicationException ex = assertThrows(ClientWebApplicationException.class, () -> client.call("a/b"));
        assertTrue(ex.getMessage().contains("Not Found"));
    }

    @Test
    public void testClientWithEncodedInParameter() {
        ClientWithEncodedInParameter client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithEncodedInParameter.class);
        assertEquals("Hello A/B", client.call("a/b"));
        assertEquals("Hello A/B/C", client.sub().call("b/c"));
    }

    @Test
    public void testClientWithEncodedInMethod() {
        ClientWithEncodedInMethod client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithEncodedInMethod.class);
        assertEquals("Hello A/B", client.call("a/b"));
        assertEquals("Hello A/B/C", client.sub1().call("b/c"));
        assertEquals("Hello A/B/C", client.sub2().call("b/c"));
        assertEquals("Hello A/B/C", client.sub3().call("b/c"));
    }

    @Test
    public void testClientWithEncodedInClass() {
        ClientWithEncodedInClass client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithEncodedInClass.class);
        assertEquals("Hello A/B", client.call("a/b"));
        assertEquals("Hello A/B/C", client.sub().call("b/c"));
    }

    @Path("/server")
    public interface ClientWithoutEncoded {
        @GET
        @Path("/{path}")
        String call(@PathParam("path") String path);

        @GET
        @Path("/{path}")
        String callWithQuery(@PathParam("path") String path, @QueryParam("query") String query);
    }

    @Path("/server")
    public interface ClientWithEncodedInParameter {
        @GET
        @Path("/{path}")
        String call(@Encoded @PathParam("path") String path);

        @Path("/a")
        SubClientWithEncodedInParameter sub();
    }

    @Path("/server")
    public interface ClientWithEncodedInMethod {
        @Encoded
        @GET
        @Path("/{path}")
        String call(@PathParam("path") String path);

        @Encoded
        @Path("/a")
        SubClientWithoutEncoded sub1();

        @Path("/a")
        SubClientWithEncodedInMethod sub2();

        @Path("/a")
        SubClientWithEncodedInClass sub3();
    }

    @Encoded
    @Path("/server")
    public interface ClientWithEncodedInClass {
        @GET
        @Path("/{path}")
        String call(@PathParam("path") String path);

        @Path("/a")
        SubClientWithoutEncoded sub();
    }

    public interface SubClientWithoutEncoded {
        @GET
        @Path("/{path}")
        String call(@PathParam("path") String path);
    }

    public interface SubClientWithEncodedInMethod {
        @Encoded
        @GET
        @Path("/{path}")
        String call(@PathParam("path") String path);
    }

    @Encoded
    public interface SubClientWithEncodedInClass {
        @GET
        @Path("/{path}")
        String call(@PathParam("path") String path);
    }

    public interface SubClientWithEncodedInParameter {
        @GET
        @Path("/{path}")
        String call(@Encoded @PathParam("path") String path);
    }

    @Path("/server")
    static class Resource {
        @GET
        @Path("/a/b")
        public String get() {
            return "Hello A/B";
        }

        @GET
        @Path("/a/b/c")
        public String getForSubResource() {
            return "Hello A/B/C";
        }
    }
}
