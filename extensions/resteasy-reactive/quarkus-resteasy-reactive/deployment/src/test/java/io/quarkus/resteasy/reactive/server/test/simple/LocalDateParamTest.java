package io.quarkus.resteasy.reactive.server.test.simple;

import java.time.LocalDate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LocalDateParamTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class));

    @Test
    public void localDateAsQueryParam() {
        RestAssured.get("/hello?date=1984-08-08")
                .then().body(Matchers.equalTo("hello#1984-08-08"));
    }

    @Test
    public void localDateAsPathParam() {
        RestAssured.get("/hello/1995-09-21")
                .then().body(Matchers.equalTo("hello@1995-09-21"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@QueryParam("date") LocalDate date) {
            return "hello#" + date;
        }

        @GET
        @Path("{date}")
        public String helloPath(@PathParam("date") LocalDate date) {
            return "hello@" + date;
        }
    }
}
