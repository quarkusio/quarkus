package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AcceptHeaderOrderTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(Resource.class));

    @Path("/headers")
    public static class Resource {
        @GET
        @Path("/amt")
        public String amt(@Context HttpHeaders hs) {
            return hs.getAcceptableMediaTypes().stream().map(MediaType::toString).collect(Collectors.joining(","));
        }
    }

    @Test
    public void preservesOrderOnTies() {
        String response = given()
                .header("Accept",
                        "application/json,text/html; charset=UTF-8,text/plain; charset=UTF-8,*/*;q=0.8")
                .get("/headers/amt")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        assertEquals("application/json,text/html;charset=UTF-8,text/plain;charset=UTF-8,*/*;q=0.8", response);
    }
}
