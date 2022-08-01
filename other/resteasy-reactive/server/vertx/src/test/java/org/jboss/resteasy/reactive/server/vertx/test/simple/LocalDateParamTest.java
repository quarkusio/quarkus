package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.DateFormat;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LocalDateParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, CustomDateTimeFormatterProvider.class));

    @Test
    public void localDateAsQueryParam() {
        RestAssured.get("/hello?date=08-08-1984")
                .then().statusCode(200).body(Matchers.equalTo("hello#1984-08-08"));
    }

    @Test
    public void localDateAsPathParam() {
        RestAssured.get("/hello/1995-09-21")
                .then().statusCode(200).body(Matchers.equalTo("hello@1995-09-21"));
    }

    @Test
    public void localDateAsFormParam() {
        RestAssured.given().formParam("date", "1995/09/22").post("/hello")
                .then().statusCode(200).body(Matchers.equalTo("hello:1995-09-22"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@QueryParam("date") @DateFormat(pattern = "dd-MM-yyyy") LocalDate date) {
            return "hello#" + date;
        }

        @GET
        @Path("{date}")
        public String helloPath(@PathParam("date") LocalDate date) {
            return "hello@" + date;
        }

        @POST
        public String helloForm(
                @FormParam("date") @DateFormat(dateTimeFormatterProvider = CustomDateTimeFormatterProvider.class) LocalDate date) {
            return "hello:" + date;
        }
    }

    public static class CustomDateTimeFormatterProvider implements DateFormat.DateTimeFormatterProvider {
        @Override
        public DateTimeFormatter get() {
            return DateTimeFormatter.ofPattern("yyyy/MM/dd");
        }
    }

}
