package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RegexMatchTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(RegexResource.class);
                }
            });

    @Test
    public void testRegexMatch() {
        RestAssured.get("/regex/1234")
                .then()
                .statusCode(200)
                .body(equalTo("pin 1234"));
        RestAssured.get("/regex/12345")
                .then()
                .statusCode(404);
    }

}
