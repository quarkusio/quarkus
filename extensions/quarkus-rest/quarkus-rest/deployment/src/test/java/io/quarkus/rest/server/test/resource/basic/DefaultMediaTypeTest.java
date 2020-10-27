package io.quarkus.rest.server.test.resource.basic;

import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.DefaultMediaTypeCustomObject;
import io.quarkus.rest.server.test.resource.basic.resource.DefaultMediaTypeResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-2847. DefaultTextPlain should be used, if produce annotation annotation is not
 *                    used in end-point.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Default Media Type Test")
public class DefaultMediaTypeTest {

    protected final Logger logger = Logger.getLogger(DefaultMediaTypeResource.class.getName());

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(DefaultMediaTypeCustomObject.class);
                    war.addClasses(PortProviderUtil.class, DefaultMediaTypeResource.class);
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
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DefaultMediaTypeResource.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test Date object with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Date Produce")
    public void postDateProduce() throws Exception {
        WebTarget target = client.target(generateURL("/postDateProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Date object without produce annotation
     *                https://issues.jboss.org/browse/RESTEASY-1403
     * @tpSince RESTEasy 3.0.16
     */
    @Test

    @DisplayName("Post Date")
    public void postDate() throws Exception {
        WebTarget target = client.target(generateURL("/postDate"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Foo object with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Foo Produce")
    public void postFooProduce() throws Exception {
        WebTarget target = client.target(generateURL("/postFooProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Foo object without produce annotation
     *                https://issues.jboss.org/browse/RESTEASY-1403
     * @tpSince RESTEasy 3.0.16
     */
    @Test

    @DisplayName("Post Foo")
    public void postFoo() throws Exception {
        WebTarget target = client.target(generateURL("/postFoo"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test int primitive with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Int Produce")
    public void postIntProduce() throws Exception {
        WebTarget target = client.target(generateURL("/postIntProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test int primitive without produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Int")
    public void postInt() throws Exception {
        WebTarget target = client.target(generateURL("/postInt"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Integer object with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Integer Produce")
    public void postIntegerProduce() throws Exception {
        WebTarget target = client.target(generateURL("/postIntegerProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Integer object without produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Integer")
    public void postInteger() throws Exception {
        WebTarget target = client.target(generateURL("/postInteger"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        logger.info(String.format("Response: %s", responseContent));
    }
}
