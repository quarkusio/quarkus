package io.quarkus.restclient.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientExceptionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(
                            new StringAsset(
                                    "io.quarkus.restclient.exception.DownstreamServiceClient/mp-rest/url=${test.url}/downstream"),
                            "application.properties")
                    .addClasses(RestClientExceptionTest.class, DownstreamServiceClient.class, FrontendService.class,
                            DownstreamServiceRedirectEndpoint.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testException() {
        Response r = ClientBuilder.newClient().target(url.toString()).path("frontend/exception").request().get();
        assertEquals(302, r.getStatus());
        assertNull(r.getLocation());
    }

    @Test
    public void testExceptionCaught() {
        Response r = ClientBuilder.newClient().target(url.toString()).path("frontend/exception-caught").request().get();
        assertEquals(302, r.getStatus());
        assertNull(r.getLocation());
    }

}
