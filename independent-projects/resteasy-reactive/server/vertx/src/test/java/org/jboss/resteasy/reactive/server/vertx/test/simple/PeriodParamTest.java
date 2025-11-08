package org.jboss.resteasy.reactive.server.vertx.test.simple;

import static org.hamcrest.Matchers.equalTo;

import java.time.Period;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class PeriodParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PeriodResource.class));

    @Test
    public void periodAsQueryParam() {
        RestAssured.get("/period?value=P1Y2M3D")
                .then().statusCode(200).body(equalTo("P1Y2M3D"));
    }

    @Test
    public void periodAsPathParam() {
        RestAssured.get("/period/P2Y")
                .then().statusCode(200).body(equalTo("P2Y"));
    }

    @Path("period")
    public static class PeriodResource {

        @GET
        public String query(@QueryParam("value") Period period) {
            return period.toString();
        }

        @GET
        @Path("{value}")
        public String path(@PathParam("value") Period period) {
            return period.toString();
        }
    }
}
