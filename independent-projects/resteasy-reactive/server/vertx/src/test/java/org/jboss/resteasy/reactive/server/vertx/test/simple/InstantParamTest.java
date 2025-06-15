package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.time.Duration;
import java.time.Instant;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class InstantParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class));

    @Test
    public void instantAsQueryParam() {
        RestAssured.get("/hello?instant=1984-08-08T01:02:03Z").then().statusCode(200)
                .body(Matchers.equalTo("hello#1984-08-09T01:02:03Z"));
    }

    @Test
    public void instantAsPathParam() {
        RestAssured.get("/hello/1984-08-08T01:02:03Z").then().statusCode(200)
                .body(Matchers.equalTo("hello@1984-08-09T01:02:03Z"));
    }

    @Test
    public void instantAsFormParam() {
        RestAssured.given().formParam("instant", "1984-08-08T01:02:03Z").post("/hello").then().statusCode(200)
                .body(Matchers.equalTo("hello:1984-08-09T01:02:03Z"));
    }

    @Test
    public void instantAsHeader() {
        RestAssured.with().header("instant", "1984-08-08T01:02:03Z").get("/hello/header").then().statusCode(200)
                .body(Matchers.equalTo("hello=1984-08-09T01:02:03Z"));
    }

    @Test
    public void instantAsCookie() {
        RestAssured.with().cookie("instant", "1984-08-08T01:02:03Z").get("/hello/cookie").then().statusCode(200)
                .body(Matchers.equalTo("hello/1984-08-09T01:02:03Z"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@RestQuery Instant instant) {
            return "hello#" + instant.plus(Duration.ofDays(1)).toString();
        }

        @GET
        @Path("{instant}")
        public String helloPath(@RestPath Instant instant) {
            return "hello@" + instant.plus(Duration.ofDays(1)).toString();
        }

        @POST
        public String helloForm(@RestForm Instant instant) {
            return "hello:" + instant.plus(Duration.ofDays(1)).toString();
        }

        @GET
        @Path("cookie")
        public String helloCookie(@RestCookie Instant instant) {
            return "hello/" + instant.plus(Duration.ofDays(1)).toString();
        }

        @GET
        @Path("header")
        public String helloHeader(@RestHeader Instant instant) {
            return "hello=" + instant.plus(Duration.ofDays(1)).toString();
        }
    }

}
