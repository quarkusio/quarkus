package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.CollectionDefaultValueResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test that empty QueryParam list is empty
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Collection Default Value Test")
public class CollectionDefaultValueTest {

    static Client client;
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(CollectionDefaultValueResource.class, PortProviderUtil.class);
                    return war;
                }
            });

    @BeforeEach
    public void init() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CollectionDefaultValueTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test that empty QueryParam list is empty
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Empty")
    public void testEmpty() throws Exception {
        Response response = client.target(generateURL("/collection")).request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }
}
