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

    @Test
    void testCookiesWithSubresource() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.cookieSub("bar", "bar2").send("bar3", "bar4")).isEqualTo("bar:bar2:bar3:bar4");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnCookieValue(@CookieParam("foo") String cookie) {
            return cookie;
        }

        @Path("2")
        @GET
        public String returnCookieValue2(@CookieParam("foo") String cookie, @CookieParam("foo2") String cookie2,
                @CookieParam("foo3") String cookie3, @CookieParam("foo4") String cookie4) {
            return cookie + ":" + cookie2 + ":" + cookie3 + ":" + cookie4;
        }
    }

    public interface Client {

        @GET
        String sendCookie(@CookieParam("foo") String cookie);

        @Path("2")
        SubClient cookieSub(@CookieParam("foo") String cookie, @CookieParam("foo2") String cookie2);
    }

    public interface SubClient {

        @GET
        String send(@CookieParam("foo3") String cookie3, @CookieParam("foo4") String cookie4);
    }

}
