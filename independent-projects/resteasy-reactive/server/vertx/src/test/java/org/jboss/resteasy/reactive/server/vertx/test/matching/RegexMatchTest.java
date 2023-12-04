package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class RegexMatchTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
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

    @Test
    public void testLiteralInRegex() {
        RestAssured.get("/regex/abb/foo/alongpathtotriggerbug")
                .then()
                .statusCode(200)
                .body(equalTo("plain:abb/foo/alongpathtotriggerbug"));
        RestAssured.get("/regex/first space/foo/second space")
                .then()
                .statusCode(200)
                .body(equalTo("plain:first space/foo/second space"));
        RestAssured.get("/regex/abb/literal/ddc")
                .then()
                .statusCode(200)
                .body(equalTo("literal:abb/ddc"));
    }
}
