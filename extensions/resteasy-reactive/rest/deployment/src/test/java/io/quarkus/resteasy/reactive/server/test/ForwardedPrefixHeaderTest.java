package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriInfo;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ForwardedPrefixHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.enable-forwarded-prefix", "true");

    @Test
    public void test() {
        when()
                .get("/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("http|http://localhost:8081/test"));

        given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Prefix", "/prefix")
                .get("/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("https|https://localhost/prefix/test"));
    }

    @Path("test")
    public static class Resource {

        @GET
        public String get(UriInfo uriInfo) {
            return uriInfo.getAbsolutePath().getScheme() + "|" + uriInfo.getAbsolutePath();
        }
    }

}
