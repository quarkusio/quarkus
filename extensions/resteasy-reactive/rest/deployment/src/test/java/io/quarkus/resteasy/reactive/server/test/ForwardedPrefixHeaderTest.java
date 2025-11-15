package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ForwardedPrefixHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.enable-forwarded-host", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.enable-forwarded-prefix", "true");

    @Test
    public void basicTest() {
        when()
                .get("/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("absolute http|http://localhost:8081/test base http|http://localhost:8081/"));

        given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "backend")
                .header("X-Forwarded-Port", "1234")
                .header("X-Forwarded-Prefix", "/prefix")
                .get("/test")
                .then()
                .statusCode(200)
                .body(Matchers
                        .equalTo("absolute https|https://backend:1234/prefix/test base https|https://backend:1234/prefix/"));
    }

    @Test
    public void redirectTest() {
        given()
                .redirects()
                .follow(false)
                .header("X-Forwarded-Prefix", "/prefix")
                .header("X-Forwarded-Host", "backend")
                .header("X-Forwarded-Port", "1234")
                .get("/test/redirect-response")
                .then()
                .statusCode(302)
                .header("Location", "http://backend:1234/prefix/new-path");

        given()
                .redirects()
                .follow(false)
                .header("X-Forwarded-Prefix", "/prefix")
                .header("X-Forwarded-Host", "backend")
                .header("X-Forwarded-Port", "1234")
                .get("/test/redirect-rest-response")
                .then()
                .statusCode(302)
                .header("Location", "http://backend:1234/prefix/new-path2");
    }

    @Path("test")
    public static class Resource {

        @GET
        public String get(UriInfo uriInfo) {
            return "absolute " + uriInfo.getAbsolutePath().getScheme() + "|" + uriInfo.getAbsolutePath() + " base "
                    + uriInfo.getBaseUri().getScheme() + "|" + uriInfo.getBaseUri();
        }

        @GET
        @Path("redirect-response")
        public Response redirectResponse() {
            return Response
                    .status(Response.Status.FOUND)
                    .location(java.net.URI.create("/new-path"))
                    .build();
        }

        @GET
        @Path("redirect-rest-response")
        public RestResponse<Object> redirectRestResponse() {
            return RestResponse.ResponseBuilder.create(Response.Status.FOUND)
                    .location(java.net.URI.create("/new-path2"))
                    .build();
        }
    }

}
