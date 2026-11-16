package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Collections;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

/**
 * The local base URI, and the {@code test.url} derived from it, must be valid when the server binds an IPv6 literal.
 */
public class IPv6LocalBaseUriTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class)
                    .add(new StringAsset("quarkus.http.host=::1\n"), "application.properties"));

    @TestHTTPResource
    URI uri;

    @Inject
    HttpServer httpServer;

    @BeforeAll
    static void assumeIpv6Loopback() throws IOException {
        NetworkInterface loopback = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        boolean ipv6 = loopback != null && Collections.list(loopback.getInetAddresses()).stream()
                .anyMatch(address -> address instanceof java.net.Inet6Address);
        Assumptions.assumeTrue(ipv6, "No IPv6 loopback address on this host");
    }

    @Test
    public void localBaseUriBracketsTheIpv6Host() {
        assertEquals("[::1]", httpServer.getLocalBaseUri().getHost());
        assertEquals(RestAssured.port, httpServer.getLocalBaseUri().getPort());
    }

    @Test
    public void testUrlBracketsTheIpv6Host() {
        assertEquals("[::1]", uri.getHost());
        assertEquals(RestAssured.port, uri.getPort());
    }

    @Test
    public void serverIsReachableThroughTheTestUrl() {
        RestAssured.given().baseUri(uri.toString()).port(uri.getPort()).get("/hello")
                .then()
                .statusCode(200)
                .body(Matchers.is("hello"));
    }

    public static class Routes {
        void init(@Observes Router router) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
