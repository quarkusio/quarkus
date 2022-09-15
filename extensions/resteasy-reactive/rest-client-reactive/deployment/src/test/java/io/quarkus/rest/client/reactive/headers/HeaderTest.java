package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class HeaderTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testHeadersWithSubresource() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.cookieSub("bar", "bar2").send("bar3", "bar4")).isEqualTo("bar:bar2:bar3:bar4");
    }

    @Test
    void testNullHeaders() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.cookieSub("bar", null).send(null, "bar4")).isEqualTo("bar:null:null:bar4");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnHeaders(@HeaderParam("foo") String header, @HeaderParam("foo2") String header2,
                @HeaderParam("foo3") String header3, @HeaderParam("foo4") String header4) {
            return header + ":" + header2 + ":" + header3 + ":" + header4;
        }
    }

    public interface Client {

        @Path("/")
        SubClient cookieSub(@HeaderParam("foo") String cookie, @HeaderParam("foo2") String cookie2);
    }

    public interface SubClient {

        @GET
        String send(@HeaderParam("foo3") String cookie3, @HeaderParam("foo4") String cookie4);
    }

}
