package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.ResponseInfoResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Response Info Test")
public class ResponseInfoTest {

    static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(ResponseInfoTest.class);
                    war.addClasses(ResponseInfoResource.class);
                    return war;
                }
            });

    @TestHTTPResource
    URI uri;

    private void basicTest(String path) {
        WebTarget base = client.target(UriBuilder.fromUri(uri).path(path));
        Response response = base.request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check URI location from HTTP headers from response prepared in resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Uri Info")
    public void testUriInfo() {
        basicTest("/simple");
    }
}
