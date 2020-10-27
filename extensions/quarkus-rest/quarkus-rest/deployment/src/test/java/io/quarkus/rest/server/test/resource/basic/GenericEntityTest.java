package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.GenericEntityDoubleWriter;
import io.quarkus.rest.server.test.resource.basic.resource.GenericEntityResource;
import io.quarkus.rest.server.test.resource.basic.resource.GenericEntitytFloatWriter;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Generic Entity Test")
public class GenericEntityTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, GenericEntityResource.class, GenericEntityDoubleWriter.class,
                            GenericEntitytFloatWriter.class);
                    return war;
                }
            });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, GenericEntityTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Resource returning GenericEntity with custom MessageBodyWriter returning double values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Doubles")
    public void testDoubles() {
        WebTarget base = client.target(generateURL("/doubles"));
        try {
            Response response = base.request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            String body = response.readEntity(String.class);
            Assertions.assertEquals("45.0D 50.0D ", body, "The response doesn't contain the expected entity");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Resource returning GenericEntity with custom MessageBodyWriter returning float values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Floats")
    public void testFloats() {
        WebTarget base = client.target(generateURL("/floats"));
        try {
            Response response = base.request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            String body = response.readEntity(String.class);
            Assertions.assertEquals("45.0F 50.0F ", body, "The response doesn't contain the expected entity");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
