package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
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

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.WiderMappingDefaultOptions;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.WiderMappingResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test negative scenario for "resteasy.wider.request.matching" property
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Wider Mapping Negative Test")
public class WiderMappingNegativeTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(WiderMappingResource.class, WiderMappingDefaultOptions.class);
                    return war;
                }
            });

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Two resources used, more general resource should not be used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Options")
    public void testOptions() {
        Response response = client.target(UriBuilder.fromUri(uri).path("/hello/int")).request().options();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertNotEquals("hello", response.readEntity(String.class));
        response.close();
    }
}
