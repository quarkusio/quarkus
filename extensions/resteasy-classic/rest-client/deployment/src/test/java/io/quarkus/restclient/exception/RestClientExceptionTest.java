package io.quarkus.restclient.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientExceptionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowWarningLogMessages(true)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
        Client client = ClientBuilder.newClient();
        try {
            Response r = client.target(url.toString()).path("frontend/exception").request().get();
            assertEquals(302, r.getStatus());
            assertNull(r.getLocation());
        } finally {
            client.close();
        }
    }

    @Test
    public void testExceptionCaught() {
        Client client = ClientBuilder.newClient();
        try {
            Response r = client.target(url.toString()).path("frontend/exception-caught").request().get();
            assertEquals(302, r.getStatus());
            assertNull(r.getLocation());
        } finally {
            client.close();
        }
    }

}
