package io.quarkus.resteasy.reactive.server.test.preconditions;

import static io.restassured.RestAssured.get;

import java.time.Instant;
import java.util.Date;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DatePreconditionTests {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class));

    // Make sure we test a subtype of Date, since that is what Hibernate ORM gives us most of the time (hah)
    // Also make sure we have non-zero milliseconds, since that will be the case for most date values representing
    // "now", and we want to make sure pre-conditions work (second-resolution)
    static final Date date = new Date(Date.from(Instant.parse("2007-12-03T10:15:30.24Z")).getTime()) {
    };

    public static class Something {
    }

    @Test
    public void test() {
        get("/preconditions")
                .then()
                .statusCode(200)
                .header("Last-Modified", "Mon, 03 Dec 2007 10:15:30 GMT")
                .body(Matchers.equalTo("foo"));
        RestAssured
                .with()
                .header("If-Modified-Since", "Mon, 03 Dec 2007 10:15:30 GMT")
                .get("/preconditions")
                .then()
                .statusCode(304);
    }

    @Path("/preconditions")
    public static class Resource {
        @GET
        public Response get(Request request) {
            ResponseBuilder resp = request.evaluatePreconditions(date);
            if (resp != null) {
                return resp.build();
            }
            return Response.ok("foo").lastModified(date).build();
        }
    }
}
