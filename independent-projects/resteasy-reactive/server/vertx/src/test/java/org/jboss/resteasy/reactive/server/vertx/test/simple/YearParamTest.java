package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.time.Year;
import java.time.format.DateTimeFormatter;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.DateFormat;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class YearParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(HelloResource.class, CustomDateTimeFormatterProvider.class));

    @Test
    public void yearAsQueryParam() {
        RestAssured.get("/hello?date=01984").then().statusCode(200).body(Matchers.equalTo("hello#1984"));
    }

    @Test
    public void yearAsPathParam() {
        RestAssured.get("/hello/1821").then().statusCode(200).body(Matchers.equalTo("hello@1821"));
    }

    @Test
    public void yearAsFormParam() {
        RestAssured.given().formParam("date", "995").post("/hello").then().statusCode(200)
                .body(Matchers.equalTo("hello:995"));
    }

    @Test
    public void yearAsHeader() {
        RestAssured.with().header("date", "1984").get("/hello/header").then().statusCode(200)
                .body(Matchers.equalTo("hello=1984"));
    }

    @Test
    public void yearAsCookie() {
        RestAssured.with().cookie("date", "1984").get("/hello/cookie").then().statusCode(200)
                .body(Matchers.equalTo("hello/1984"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@QueryParam("date") @DateFormat(pattern = "yyyyy") Year date) {
            return "hello#" + date;
        }

        @GET
        @Path("{date}")
        public String helloPath(@PathParam("date") Year date) {
            return "hello@" + date;
        }

        @POST
        public String helloForm(
                @FormParam("date") @DateFormat(dateTimeFormatterProvider = CustomDateTimeFormatterProvider.class) Year date) {
            return "hello:" + date;
        }

        @GET
        @Path("cookie")
        public String helloCookie(@CookieParam("date") Year date) {
            return "hello/" + date;
        }

        @GET
        @Path("header")
        public String helloHeader(@HeaderParam("date") Year date) {
            return "hello=" + date;
        }
    }

    public static class CustomDateTimeFormatterProvider implements DateFormat.DateTimeFormatterProvider {
        @Override
        public DateTimeFormatter get() {
            return DateTimeFormatter.ofPattern("yyy");
        }
    }

}
