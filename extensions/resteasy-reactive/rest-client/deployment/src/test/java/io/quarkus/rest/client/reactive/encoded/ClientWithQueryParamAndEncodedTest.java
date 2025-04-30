package io.quarkus.rest.client.reactive.encoded;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientWithQueryParamAndEncodedTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    @Test
    public void testClientWithoutEncoded() {
        ClientWithoutEncoded client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithoutEncoded.class);
        assertEquals("Hello query=%2524value", client.call("%24value"));
    }

    @Test
    public void testClientWithEncodedInParameter() {
        ClientWithEncodedInParameter client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithEncodedInParameter.class);
        assertEquals("Hello query=%24value", client.call("%24value"));
        assertEquals("Hello query1=%2524value&query2=%24value&query3=%2524value",
                client.call("%24value", "%24value", "%24value"));
        assertEquals("Hello subQuery=%24value", client.sub().call("%24value"));
    }

    @Test
    public void testClientWithEncodedInMethod() {
        ClientWithEncodedInMethod client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithEncodedInMethod.class);
        assertEquals("Hello query=%24value", client.call("%24value"));
        assertEquals("Hello subQuery=%24value", client.sub1().call("%24value"));
        assertEquals("Hello subQuery=%24value", client.sub2().call("%24value"));
        assertEquals("Hello subQuery=%24value", client.sub3().call("%24value"));
    }

    @Test
    public void testClientWithEncodedInClass() {
        ClientWithEncodedInClass client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(ClientWithEncodedInClass.class);
        assertEquals("Hello query=%24value", client.call("%24value"));
        assertEquals("Hello subQuery=%24value", client.sub().call("%24value"));
    }

    @Path("/server")
    public interface ClientWithoutEncoded {
        @GET
        String call(@QueryParam("query") String query);
    }

    @Path("/server")
    public interface ClientWithEncodedInParameter {
        @GET
        String call(@Encoded @QueryParam("query") String query);

        @GET
        String call(@QueryParam("query1") String query1, @Encoded @QueryParam("query2") String query2,
                @QueryParam("query3") String query3);

        @Path("/a")
        SubClientWithEncodedInParameter sub();
    }

    @Path("/server")
    public interface ClientWithEncodedInMethod {
        @Encoded
        @GET
        String call(@QueryParam("query") String query);

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
        String call(@QueryParam("query") String query);

        @Path("/a")
        SubClientWithoutEncoded sub();
    }

    public interface SubClientWithoutEncoded {
        @GET
        String call(@QueryParam("subQuery") String subQuery);
    }

    @Encoded
    public interface SubClientWithEncodedInClass {
        @GET
        String call(@QueryParam("subQuery") String subQuery);
    }

    public interface SubClientWithEncodedInMethod {
        @Encoded
        @GET
        String call(@QueryParam("subQuery") String subQuery);
    }

    public interface SubClientWithEncodedInParameter {
        @GET
        String call(@Encoded @QueryParam("subQuery") String subQuery);
    }

    @Path("/server")
    static class Resource {
        @GET
        public String get(@Context UriInfo uriInfo) {
            return "Hello " + uriInfo.getRequestUri().getRawQuery();
        }

        @GET
        @Path("/a")
        public String getFromA(@Context UriInfo uriInfo) {
            return "Hello " + uriInfo.getRequestUri().getRawQuery();
        }
    }
}
