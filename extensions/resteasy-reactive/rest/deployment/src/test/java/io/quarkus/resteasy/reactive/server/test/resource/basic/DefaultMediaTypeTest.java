package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.DefaultMediaTypeCustomObject;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.DefaultMediaTypeResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-2847. DefaultTextPlain should be used, if produce annotation is not
 *                    used in end-point.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Default Media Type Test")
public class DefaultMediaTypeTest {

    private final Logger LOG = Logger.getLogger(DefaultMediaTypeResource.class.getName());

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(DefaultMediaTypeCustomObject.class);
                    war.addClasses(DefaultMediaTypeResource.class);
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

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Test Date object with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Date Produce")
    public void postDateProduce() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postDateProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Date object without produce annotation
     *                https://issues.jboss.org/browse/RESTEASY-1403
     * @tpSince RESTEasy 3.0.16
     */
    @Test

    @DisplayName("Post Date")
    public void postDate() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postDate"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Foo object with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Foo Produce")
    public void postFooProduce() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postFooProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Foo object without produce annotation
     *                https://issues.jboss.org/browse/RESTEASY-1403
     * @tpSince RESTEasy 3.0.16
     */
    @Test

    @DisplayName("Post Foo")
    public void postFoo() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postFoo"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test int primitive with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Int Produce")
    public void postIntProduce() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postIntProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test int primitive without produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Int")
    public void postInt() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postInt"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Integer object with produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Integer Produce")
    public void postIntegerProduce() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postIntegerProduce"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }

    /**
     * @tpTestDetails Test Integer object without produce annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Post Integer")
    public void postInteger() {
        WebTarget target = client.target(UriBuilder.fromUri(uri).path("/postInteger"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
        for (int i = 0; i < 5000; i++) {
            baos.write(i);
        }
        Response response = target.request().post(Entity.entity(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        String responseContent = response.readEntity(String.class);
        LOG.debug(String.format("Response: %s", responseContent));
    }
}
