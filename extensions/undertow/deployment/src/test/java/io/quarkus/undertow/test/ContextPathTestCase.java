package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class ContextPathTestCase {

    private static final String CONTEXT_PATH = "/foo";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestServlet.class)
                    .addAsResource(new StringAsset("quarkus.servlet.context-path=" + CONTEXT_PATH), "application.properties"));

    @TestHTTPResource
    String url;

    @Test
    public void testServlet() {
        assertTrue(System.getProperty("test.url").endsWith(CONTEXT_PATH));
        assertTrue(url.endsWith(CONTEXT_PATH + "/"));
        assertEquals(CONTEXT_PATH, RestAssured.basePath);
        RestAssured.when().get("/test").then()
                .statusCode(200)
                .body(is("test servlet"));
    }

}
