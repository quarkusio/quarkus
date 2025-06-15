package org.jboss.resteasy.reactive.server.vertx.test.response;

import static org.hamcrest.CoreMatchers.endsWith;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class RestResponseTest {
    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(RestResponseResource.class,
                    UnknownCheeseException1.class, UnknownCheeseException2.class);
        }
    });

    @Test
    public void test() {
        RestAssured.get("/rest-response").then().statusCode(200).and().body(Matchers.equalTo("Hello")).and()
                .contentType("text/plain");
        RestAssured.get("/rest-response-empty").then().statusCode(499).and()
                .body(Matchers.is(Matchers.emptyOrNullString())).and()
                .contentType(Matchers.is(Matchers.emptyOrNullString()));
        RestAssured.get("/response-empty").then().statusCode(499).and().body(Matchers.is(Matchers.emptyOrNullString()))
                .and().contentType(Matchers.is(Matchers.emptyOrNullString()));
        RestAssured.get("/rest-response-wildcard").then().statusCode(200).and().body(Matchers.equalTo("Hello")).and()
                .contentType("text/plain");
        RestAssured.get("/rest-response-full").then().statusCode(200).body(Matchers.equalTo("Hello"))
                .contentType("text/stef").header("Allow", "BAR, FOO")
                .header("Cache-Control", "no-transform, max-age=42, private")
                .header("Content-Location", "http://example.com/content").cookies("Flavour", "Praliné")
                .header("Content-Encoding", "Stef-Encoding").header("Expires", "Fri, 01 Jan 2021 00:00:00 GMT")
                .header("X-Stef", "FroMage").header("Content-Language", "fr")
                .header("Last-Modified", "Sat, 02 Jan 2021 00:00:00 GMT")
                .header("Link", "<http://example.com/link>; rel=\"stef\"")
                .header("Location", "http://example.com/location").header("ETag", "\"yourit\"")
                .header("Vary", "Accept-Language");
        // RestAssured.get("/response-uni")
        // .then().statusCode(200)
        // .and().body(Matchers.equalTo("Hello"))
        // .and().contentType("text/plain");
        // RestAssured.get("/rest-response-uni")
        // .then().statusCode(200)
        // .and().body(Matchers.equalTo("Hello"))
        // .and().contentType("text/plain");
        RestAssured.get("/rest-response-exception").then().statusCode(404).and()
                .body(Matchers.equalTo("Unknown cheese: Cheddar")).and().contentType("text/plain");
        RestAssured.get("/uni-rest-response-exception").then().statusCode(404).and()
                .body(Matchers.equalTo("Unknown cheese: Cheddar")).and().contentType("text/plain");
        RestAssured.get("/rest-response-request-filter").then().statusCode(200).and()
                .body(Matchers.equalTo("RestResponse request filter")).and().contentType("text/plain");
        RestAssured.get("/optional-rest-response-request-filter").then().statusCode(200).and()
                .body(Matchers.equalTo("Optional<RestResponse> request filter")).and().contentType("text/plain");
        RestAssured.get("/uni-rest-response-request-filter").then().statusCode(200).and()
                .body(Matchers.equalTo("Uni<RestResponse> request filter")).and().contentType("text/plain");
        RestAssured.get("/rest-response-exception").then().statusCode(404).and()
                .body(Matchers.equalTo("Unknown cheese: Cheddar")).and().contentType("text/plain");
        RestAssured.get("/uni-rest-response-exception").then().statusCode(404).and()
                .body(Matchers.equalTo("Unknown cheese: Cheddar")).and().contentType("text/plain");
        RestAssured.get("/rest-response-request-filter").then().statusCode(200).and()
                .body(Matchers.equalTo("RestResponse request filter")).and().contentType("text/plain");
        RestAssured.get("/optional-rest-response-request-filter").then().statusCode(200).and()
                .body(Matchers.equalTo("Optional<RestResponse> request filter")).and().contentType("text/plain");
        RestAssured.get("/uni-rest-response-request-filter").then().statusCode(200).and()
                .body(Matchers.equalTo("Uni<RestResponse> request filter")).and().contentType("text/plain");
        RestAssured.get("/rest-response-location").then().statusCode(200).header("Location",
                endsWith("/en%2Fus?user=John"));
        RestAssured.get("/rest-response-content-location").then().statusCode(200).header("Content-Location",
                endsWith("/en%2Fus?user=John"));
    }
}
