package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
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

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.MatchedResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression tests for RESTEASY-549 and RESTEASY-537
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Matched Resource Test")
public class MatchedResourceTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(MatchedResource.class);
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
    }

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Regression test for RESTEASY-549
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Empty")
    public void testEmpty() {
        WebTarget base = client.target(UriBuilder.fromUri(uri).path("/start"));
        Response response = base.request().post(Entity.text(""));
        Assertions.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        String rtn = response.readEntity(String.class);
        Assertions.assertEquals("started", rtn);
        response.close();
        base = client.target(UriBuilder.fromUri(uri).path("/start"));
        response = base.request().post(Entity.entity("<xml/>", "application/xml"));
        Assertions.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        rtn = response.readEntity(String.class);
        Assertions.assertEquals("<xml/>", rtn, "Wrong response content");
        response.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-537
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Match")
    public void testMatch() {
        WebTarget base = client.target(UriBuilder.fromUri(uri).path("/match"));
        Response response = base.request().header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .get();
        Assertions.assertEquals("text/html;charset=UTF-8", response.getHeaders().getFirst("Content-Type"));
        String res = response.readEntity(String.class);
        Assertions.assertEquals("*/*", res, "Wrong response content");
        response.close();
    }

    public void generalPostTest(String path, String value) {
        WebTarget base = client.target(UriBuilder.fromUri(uri).path(path));
        Response response = base.request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), value, "Wrong response content");
    }

    /**
     * @tpTestDetails Check post request on resource with @GET annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post")
    public void testPost() {
        generalPostTest("/test2/foo.xml.en", "complex2");
        generalPostTest("/test1/foo.xml.en", "complex");
    }
}
