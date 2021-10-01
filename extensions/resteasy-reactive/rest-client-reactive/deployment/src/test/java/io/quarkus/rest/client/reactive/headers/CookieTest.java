package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class CookieTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testCookie() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendCookie("bar")).isEqualTo("bar");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnCookieValue(@CookieParam("foo") String cookie) {
            return cookie;
        }
    }

    public interface Client {

        @GET
        String sendCookie(@CookieParam("foo") String cookie);
    }

}
