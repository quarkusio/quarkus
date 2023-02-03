package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestHeader;
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
        assertThat(client.cookieSub("bar", "bar2").send("bar3", "bar4", "dummy"))
                .isEqualTo("bar:bar2:bar3:bar4:X-My-Header/dummy");
    }

    @Test
    void testNullHeaders() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.cookieSub("bar", null).send(null, "bar4", "dummy")).isEqualTo("bar:null:null:bar4:X-My-Header/dummy");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnHeaders(@HeaderParam("foo") String header, @HeaderParam("foo2") String header2,
                @HeaderParam("foo3") String header3, @HeaderParam("foo4") String header4, @Context HttpHeaders headers) {
            String myHeaderName = headers.getRequestHeaders().keySet().stream().filter(s -> s.equalsIgnoreCase("X-My-Header"))
                    .findFirst().orElse("");
            return header + ":" + header2 + ":" + header3 + ":" + header4 + ":" + myHeaderName + "/"
                    + headers.getHeaderString("X-My-Header");
        }
    }

    public interface Client {

        @Path("/")
        SubClient cookieSub(@HeaderParam("foo") String cookie, @HeaderParam("foo2") String cookie2);
    }

    public interface SubClient {

        @GET
        String send(@HeaderParam("foo3") String cookie3, @HeaderParam("foo4") String cookie4, @RestHeader String xMyHeader);
    }

}
