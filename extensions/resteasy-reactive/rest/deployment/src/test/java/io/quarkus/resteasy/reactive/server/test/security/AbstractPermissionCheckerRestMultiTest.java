package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestMulti;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.test.utils.TestIdentityController;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;

public abstract class AbstractPermissionCheckerRestMultiTest {

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles().add("user", "user");
    }

    @Test
    public void testReturnRestMultiAsMulti() {
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .queryParam("user", "Georgios")
                .get("/test/public-multi")
                .then()
                .statusCode(201)
                .header("header1", "value header 1");
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .queryParam("user", "Georgios")
                .get("/test/secured-multi")
                .then()
                .statusCode(201)
                .header("header1", "value header 1");
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .queryParam("user", "Sergey")
                .get("/test/secured-multi")
                .then()
                .statusCode(403)
                .header("header1", Matchers.nullValue());
    }

    @Test
    public void testReturnRestMulti() {
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .queryParam("user", "Georgios")
                .get("/test/secured-rest-multi")
                .then()
                .statusCode(201)
                .header("header1", "value header 1");
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .queryParam("user", "Sergey")
                .get("/test/secured-rest-multi")
                .then()
                .statusCode(403)
                .header("header1", Matchers.nullValue());
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("public-multi")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<Byte> publicMethod(@QueryParam("user") String user) {
            return RestMulti.fromMultiData(Multi.createFrom().<Byte> empty())
                    .status(201)
                    .header("header1", "value header 1")
                    .build();
        }

        @PermissionsAllowed("secured")
        @GET
        @Path("secured-multi")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<Byte> securedMethod(@QueryParam("user") String user) {
            return RestMulti.fromMultiData(Multi.createFrom().<Byte> empty())
                    .status(201)
                    .header("header1", "value header 1")
                    .build();
        }

        @PermissionsAllowed("secured")
        @GET
        @Path("secured-rest-multi")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public RestMulti<Byte> securedMethodRestMulti(@QueryParam("user") String user) {
            return RestMulti.fromMultiData(Multi.createFrom().<Byte> empty())
                    .status(201)
                    .header("header1", "value header 1")
                    .build();
        }

        @PermissionChecker(value = "secured")
        public boolean canCallSecured(String user) {
            return "Georgios".equals(user);
        }
    }
}
