package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class OtherJavaTimeParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class));

    @Test
    public void localTimeParam() {
        RestAssured.get("/hello/localTime?date=01:02:03")
                .then().statusCode(200).body(Matchers.equalTo("hello#1:2:3"));
    }

    @Test
    public void offsetDateTime() {
        RestAssured.get("/hello/offsetDateTime?date=1995-09-21T01:02:03+01:00")
                .then().statusCode(200).body(Matchers.equalTo("hello#1995:+01:00"));
    }

    @Test
    public void offsetTime() {
        RestAssured.get("/hello/offsetTime?date=11:02:03+01:00")
                .then().statusCode(200).body(Matchers.equalTo("hello#+01:00"));
    }

    @Test
    public void zonedDateTime() {
        RestAssured.get("/hello/zonedDateTime?date=1995-09-21T01:02:03+01:00[Europe/Paris]")
                .then().statusCode(200).body(Matchers.equalTo("hello#1995:9"));
    }

    @Path("hello")
    public static class HelloResource {

        @Path("localTime")
        @GET
        public String localTime(@QueryParam("date") LocalTime date) {
            return "hello#" + date.getHour() + ":" + date.getMinute() + ":" + date.getSecond();
        }

        @Path("offsetDateTime")
        @GET
        public String offsetDateTime(@QueryParam("date") OffsetDateTime date) {
            return "hello#" + date.getYear() + ":" + date.getOffset().getId();
        }

        @Path("offsetTime")
        @GET
        public String offsetTime(@QueryParam("date") OffsetTime date) {
            return "hello#" + date.getOffset().getId();
        }

        @Path("zonedDateTime")
        @GET
        public String zonedDateTime(@QueryParam("date") ZonedDateTime date) {
            return "hello#" + date.getYear() + ":" + date.getMonth().getValue();
        }
    }
}
