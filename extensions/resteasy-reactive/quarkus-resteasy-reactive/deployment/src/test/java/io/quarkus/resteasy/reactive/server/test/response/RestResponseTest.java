package io.quarkus.resteasy.reactive.server.test.response;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RestResponseTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(RestResponseResource.class, UnknownCheeseException1.class,
                                    UnknownCheeseException2.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/rest-response")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("Hello"))
                .and().contentType("text/plain");
        RestAssured.get("/rest-response-full")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello"))
                .contentType("text/stef")
                .header("Allow", "BAR, FOO")
                .header("Cache-Control", "no-transform, max-age=42, private")
                .header("Content-Location", "http://example.com/content")
                .cookies("Flavour", "Praliné")
                .header("Content-Encoding", "Stef-Encoding")
                .header("Expires", "Fri, 01 Jan 2021 00:00:00 GMT")
                .header("X-Stef", "FroMage")
                .header("Content-Language", "fr")
                .header("Last-Modified", "Sat, 02 Jan 2021 00:00:00 GMT")
                .header("Link", "<http://example.com/link>; rel=\"stef\"")
                .header("Location", "http://example.com/location")
                .header("ETag", "\"yourit\"")
                .header("Vary", "Accept-Language");
        //        RestAssured.get("/response-uni")
        //        .then().statusCode(200)
        //        .and().body(Matchers.equalTo("Hello"))
        //        .and().contentType("text/plain");
        //        RestAssured.get("/rest-response-uni")
        //        .then().statusCode(200)
        //        .and().body(Matchers.equalTo("Hello"))
        //        .and().contentType("text/plain");
        RestAssured.get("/rest-response-exception")
                .then().statusCode(404)
                .and().body(Matchers.equalTo("Unknown cheese: Cheddar"))
                .and().contentType("text/plain");
        RestAssured.get("/uni-rest-response-exception")
                .then().statusCode(404)
                .and().body(Matchers.equalTo("Unknown cheese: Cheddar"))
                .and().contentType("text/plain");
        RestAssured.get("/rest-response-request-filter")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("RestResponse request filter"))
                .and().contentType("text/plain");
        RestAssured.get("/optional-rest-response-request-filter")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("Optional<RestResponse> request filter"))
                .and().contentType("text/plain");
        RestAssured.get("/uni-rest-response-request-filter")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("Uni<RestResponse> request filter"))
                .and().contentType("text/plain");
    }
}
