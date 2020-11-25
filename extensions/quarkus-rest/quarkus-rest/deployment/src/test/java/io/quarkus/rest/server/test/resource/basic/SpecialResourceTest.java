package io.quarkus.rest.server.test.resource.basic;

import java.io.IOException;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.SpecialResourceApiResource;
import io.quarkus.rest.server.test.resource.basic.resource.SpecialResourceDeleteResource;
import io.quarkus.rest.server.test.resource.basic.resource.SpecialResourceStreamResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEasy issues about special resources
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Special Resource Test")
public class SpecialResourceTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(SpecialResourceStreamResource.class, SpecialResourceApiResource.class,
                            PortProviderUtil.class,
                            SpecialResourceDeleteResource.class);
                    return war;
                }
            });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SpecialResourceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-631
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 631")
    public void test631() throws Exception {
        WebTarget base = client.target(generateURL("/delete"));
        Response response = base.request().method("DELETE", Entity.entity("hello", "text/plain"));
        Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-534
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 534")
    public void test534() throws Exception {
        WebTarget base = client.target(generateURL("/inputstream/test/json"));
        Response response = base.request().post(Entity.entity("hello world".getBytes(), MediaType.APPLICATION_OCTET_STREAM));
        Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-624
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 624")
    public void test624() throws Exception {
        WebTarget base = client.target(generateURL("/ApI/FuNc"));
        Response response = base.request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-583
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 583")
    public void test583() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPut method = new HttpPut(generateURL("/api"));
        HttpResponse response = null;
        try {
            method.setEntity(
                    new StringEntity("hello", ContentType.create("vnd.net.juniper.space.target-management.targets+xml")));
            response = client.execute(method);
            Assertions.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (response != null) {
                    EntityUtils.consume(response.getEntity());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
