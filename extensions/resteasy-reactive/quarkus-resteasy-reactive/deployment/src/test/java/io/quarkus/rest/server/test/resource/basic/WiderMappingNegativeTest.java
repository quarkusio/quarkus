package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.WiderMappingDefaultOptions;
import io.quarkus.rest.server.test.resource.basic.resource.WiderMappingResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test negative scenario for "resteasy.wider.request.matching" property
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Wider Mapping Negative Test")
public class WiderMappingNegativeTest {

    static Client client;

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, WiderMappingNegativeTest.class.getSimpleName());
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, WiderMappingResource.class, WiderMappingDefaultOptions.class);
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

    /**
     * @tpTestDetails Two resources used, more general resource should not be used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Options")
    public void testOptions() {
        Response response = client.target(generateURL("/hello/int")).request().options();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertNotEquals(response.readEntity(String.class), "hello");
        response.close();
    }
}
