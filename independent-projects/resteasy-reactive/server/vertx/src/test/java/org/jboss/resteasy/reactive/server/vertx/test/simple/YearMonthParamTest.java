package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.time.YearMonth;
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

public class YearMonthParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, CustomDateTimeFormatterProvider.class));

    @Test
    public void yearMonthAsQueryParam() {
        RestAssured.get("/hello?date=01984-12")
                .then().statusCode(200).body(Matchers.equalTo("hello#1984-12"));
    }

    @Test
    public void yearMonthAsPathParam() {
        RestAssured.get("/hello/1821-01")
                .then().statusCode(200).body(Matchers.equalTo("hello@1821-01"));
    }

    @Test
    public void yearMonthAsFormParam() {
        RestAssured.given().formParam("date", "995-06").post("/hello")
                .then().statusCode(200).body(Matchers.equalTo("hello:0995-06"));
    }

    @Test
    public void yearMonthAsHeader() {
        RestAssured.with().header("date", "1984-11")
                .get("/hello/header")
                .then().statusCode(200).body(Matchers.equalTo("hello=1984-11"));
    }

    @Test
    public void yearMonthAsCookie() {
        RestAssured.with().cookie("date", "1984-10")
                .get("/hello/cookie")
                .then().statusCode(200).body(Matchers.equalTo("hello/1984-10"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@QueryParam("date") @DateFormat(pattern = "yyyyy-MM") YearMonth date) {
            return "hello#" + date;
        }

        @GET
        @Path("{date}")
        public String helloPath(@PathParam("date") YearMonth date) {
            return "hello@" + date;
        }

        @POST
        public String helloForm(
                @FormParam("date") @DateFormat(dateTimeFormatterProvider = CustomDateTimeFormatterProvider.class) YearMonth date) {
            return "hello:" + date;
        }

        @GET
        @Path("cookie")
        public String helloCookie(
                @CookieParam("date") YearMonth date) {
            return "hello/" + date;
        }

        @GET
        @Path("header")
        public String helloHeader(
                @HeaderParam("date") YearMonth date) {
            return "hello=" + date;
        }
    }

    public static class CustomDateTimeFormatterProvider implements DateFormat.DateTimeFormatterProvider {
        @Override
        public DateTimeFormatter get() {
            return DateTimeFormatter.ofPattern("yyy-MM");
        }
    }

}
