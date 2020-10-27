package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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

import io.quarkus.rest.server.test.resource.basic.resource.MatchedResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

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
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(MatchedResource.class, PortProviderUtil.class);
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

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MatchedResourceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-549
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Empty")
    public void testEmpty() throws Exception {
        WebTarget base = client.target(generateURL("/start"));
        Response response = base.request().post(Entity.text(""));
        Assertions.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        String rtn = response.readEntity(String.class);
        Assertions.assertEquals("started", rtn);
        response.close();
        base = client.target(generateURL("/start"));
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
    public void testMatch() throws Exception {
        WebTarget base = client.target(generateURL("/match"));
        Response response = base.request().header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .get();
        Assertions.assertEquals("text/html", response.getHeaders().getFirst("Content-Type"));
        String res = response.readEntity(String.class);
        Assertions.assertEquals("*/*", res, "Wrong response content");
        response.close();
    }

    public void generalPostTest(String uri, String value) {
        WebTarget base = client.target(uri);
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
        generalPostTest(generateURL("/test2/foo.xml.en"), "complex2");
        generalPostTest(generateURL("/test1/foo.xml.en"), "complex");
    }
}
