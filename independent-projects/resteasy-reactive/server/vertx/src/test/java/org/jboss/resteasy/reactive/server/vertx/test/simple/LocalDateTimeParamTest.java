package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.DateFormat;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class LocalDateTimeParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class, CustomDateTimeFormatterProvider.class));

    @Test
    public void localDateTimeAsQueryParam() {
        RestAssured.get("/hello?date=1984-08-08T01:02:03")
                .then().statusCode(200).body(Matchers.equalTo("hello#1984"));
    }

    @Test
    public void localDateTimeAsOptionalQueryParam() {
        RestAssured.get("/hello/optional?date=1984-08-08T01:02:03")
                .then().statusCode(200).body(Matchers.equalTo("hello#1984"));

        RestAssured.get("/hello/optional")
                .then().statusCode(200).body(Matchers.equalTo("hello#2022"));
    }

    @Test
    public void localDateTimeAsPathParam() {
        RestAssured.get("/hello/1995-09-21 01:02:03")
                .then().statusCode(200).body(Matchers.equalTo("hello@9"));
    }

    @Test
    public void localDateTimeAsFormParam() {
        RestAssured.given().formParam("date", "1995/09/22 01:02").post("/hello")
                .then().statusCode(200).body(Matchers.equalTo("hello:22"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@RestQuery LocalDateTime date) {
            return "hello#" + date.getYear();
        }

        @Path("optional")
        @GET
        public String helloOptionalQuery(@RestQuery Optional<LocalDateTime> date) {
            return "hello#" + date.orElse(LocalDateTime.of(2022, 1, 1, 0, 0)).getYear();
        }

        @GET
        @Path("{date}")
        public String helloPath(@RestPath @DateFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime date) {
            return "hello@" + date.getMonthValue();
        }

        @POST
        public String helloForm(
                @FormParam("date") @DateFormat(dateTimeFormatterProvider = CustomDateTimeFormatterProvider.class) LocalDateTime date) {
            return "hello:" + date.getDayOfMonth();
        }
    }

    public static class CustomDateTimeFormatterProvider implements DateFormat.DateTimeFormatterProvider {
        @Override
        public DateTimeFormatter get() {
            return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        }
    }

}
