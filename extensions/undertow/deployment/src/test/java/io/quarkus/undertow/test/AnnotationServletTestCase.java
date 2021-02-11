package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AnnotationServletTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestServlet.class, TestServletSubclass.class, TestGreeter.class));

    @Test
    public void testServlet() {
        RestAssured.when().get("/test").then()
                .statusCode(200)
                .body(is("test servlet"));
    }

    @Test
    public void testServletSubclass() {
        RestAssured.when().get("/test-sub").then()
                .statusCode(200)
                .body(is("test servlet"));
    }
}
