package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StemMatchTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SpecificResource.class, GeneralResource.class);
                }
            });

    @Test
    public void testPathMatchSpecifics() {
        given()
                .when().get("/hello/foo")
                .then()
                .statusCode(200)
                .body(is("general:foo"));
        given()
                .when().get("/hello/foo/bar")
                .then()
                .statusCode(200)
                .body(is("specific:foo"));
    }

}
