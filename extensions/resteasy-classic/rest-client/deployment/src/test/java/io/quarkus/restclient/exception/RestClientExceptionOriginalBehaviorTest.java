package io.quarkus.restclient.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ExpectLogMessage;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientExceptionOriginalBehaviorTest {

    static StringAsset createStringAsset() {
        try {
            Properties props = new Properties();
            props.put("io.quarkus.restclient.exception.DownstreamServiceClient/mp-rest/url",
                    "${test.url}/downstream");
            props.put("resteasy.original.webapplicationexception.behavior", "true");
            StringWriter sw = new StringWriter();
            props.store(sw, "application.properties");
            return new StringAsset(sw.toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(createStringAsset(), "application.properties")
                    .addClasses(RestClientExceptionOriginalBehaviorTest.class, DownstreamServiceClient.class,
                            FrontendService.class,
                            DownstreamServiceUnavailableEndpoint.class));

    @TestHTTPResource
    URL url;

    @Test
    @ExpectLogMessage("Service Unavailable")
    public void testException() {
        Client client = ClientBuilder.newClient();
        try {
            Response r = client.target(url.toString()).path("frontend/exception").request().get();
            assertEquals(503, r.getStatus());
            assertEquals("5", r.getHeaderString("Retry-After"));
        } finally {
            client.close();
        }
    }

    @Test
    @ExpectLogMessage("Service Unavailable")
    public void testExceptionCaught() {
        Client client = ClientBuilder.newClient();
        try {
            Response r = client.target(url.toString()).path("frontend/exception-caught").request().get();
            assertEquals(503, r.getStatus());
            assertEquals("5", r.getHeaderString("Retry-After"));
        } finally {
            client.close();
        }
    }

}
