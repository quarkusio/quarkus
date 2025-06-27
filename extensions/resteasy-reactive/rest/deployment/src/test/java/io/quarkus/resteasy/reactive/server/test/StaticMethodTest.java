package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class StaticMethodTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(StaticMethod.class, FooNoClassPathStaticMethod.class);
                }
            });

    @Test
    public void testStaticMethod() {
        given().when().get("hello")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"));
        given().when().get("noclass")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("noclass"));
    }

    @Path("/")
    public static class StaticMethod {

        @Path("hello")
        @GET
        public static String hello() {
            return "hello";
        }
    }
}
