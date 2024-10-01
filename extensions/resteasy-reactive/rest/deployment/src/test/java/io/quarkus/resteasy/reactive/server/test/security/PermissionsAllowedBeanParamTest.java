package io.quarkus.resteasy.reactive.server.test.security;

import java.security.BasicPermission;
import java.security.Permission;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestCookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class PermissionsAllowedBeanParamTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, SimpleBeanParam.class,
                            SimpleResource.class, SimpleBeanParamPermission.class, MyPermission.class, MyBeanParam.class,
                            OtherBeanParamPermission.class, OtherBeanParam.class));

    @BeforeAll
    public static void setupUsers() {
        var sayHelloPossessedPerm = new BasicPermission("say", "hello") {
            @Override
            public boolean implies(Permission p) {
                return getName().equals(p.getName()) && getActions().equals(p.getActions());
            }

            @Override
            public String getActions() {
                return "hello";
            }
        };
        TestIdentityController.resetRoles()
                .add("admin", "admin", SimpleBeanParamPermission.EMPTY, MyPermission.EMPTY, sayHelloPossessedPerm)
                .add("user", "user", sayHelloPossessedPerm);
    }

    @Test
    public void testSimpleBeanParam() {
        getSimpleBeanParamReq()
                .post("/simple/param")
                .then().statusCode(401);
        getSimpleBeanParamReq()
                .auth().preemptive().basic("user", "user")
                .post("/simple/param")
                .then().statusCode(403);
        getSimpleBeanParamReq()
                .auth().preemptive().basic("admin", "admin")
                .post("/simple/param")
                .then().statusCode(200).body(Matchers.equalTo("OK"));
    }

    @Test
    public void testRecordBeanParam() {
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .queryParam("queryParam", "query1")
                .get("/simple/record-param")
                .then().statusCode(403);
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .queryParam("queryParam", "query1")
                .get("/simple/record-param")
                .then().statusCode(200)
                .body(Matchers.equalTo("OK"));
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .queryParam("queryParam", "wrong-query-param")
                .get("/simple/record-param")
                .then().statusCode(403);
    }

    @Test
    public void testAutodetectedParams() {
        RestAssured
                .given()
                .body("autodetected")
                .auth().preemptive().basic("admin", "admin")
                .header("CustomAuthorization", "customAuthorization")
                .queryParam("query", "myQueryParam")
                .get("/simple/autodetect-params")
                .then().statusCode(200).body(Matchers.equalTo("autodetected"));
        // wrong custom authorization
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .header("CustomAuthorization", "wrongAuthorization")
                .queryParam("query", "myQueryParam")
                .get("/simple/autodetect-params")
                .then().statusCode(403);
        // wrong query param
        RestAssured
                .given()
                .body("autodetected")
                .auth().preemptive().basic("admin", "admin")
                .header("CustomAuthorization", "customAuthorization")
                .queryParam("query", "wrongQueryParam")
                .get("/simple/autodetect-params")
                .then().statusCode(403);
        // wrong principal
        RestAssured
                .given()
                .body("autodetected")
                .auth().preemptive().basic("user", "user")
                .header("CustomAuthorization", "customAuthorization")
                .queryParam("query", "myQueryParam")
                .get("/simple/autodetect-params")
                .then().statusCode(403);
    }

    private static RequestSpecification getSimpleBeanParamReq() {
        return RestAssured
                .with()
                .header("header", "one-header")
                .queryParam("query", "one-query")
                .queryParam("queryList", "one")
                .queryParam("queryList", "two")
                .queryParam("int", "666")
                .cookie("cookie", "cookie")
                .body("OK");
    }

    @Path("/simple")
    public static class SimpleResource {

        @PermissionsAllowed(value = "perm1", permission = SimpleBeanParamPermission.class, params = { "cookie",
                "beanParam.header", "beanParam.publicQuery", "beanParam.queryList", "beanParam.securityContext",
                "beanParam.uriInfo", "beanParam.privateQuery" })
        @Path("/param")
        @POST
        public String simpleBeanParam(@BeanParam SimpleBeanParam beanParam, String payload, @RestCookie String cookie) {
            return payload;
        }

        @PermissionsAllowed(value = "perm2", permission = MyPermission.class, params = { "beanParam.queryParam",
                "beanParam.headers.authorization" })
        @Path("/record-param")
        @GET
        public String recordBeanParam(@BeanParam MyBeanParam beanParam) {
            return "OK";
        }

        @PermissionsAllowed(value = "say:hello", permission = OtherBeanParamPermission.class, params = "otherBeanParam.securityContext.userPrincipal.name")
        @Path("/autodetect-params")
        @GET
        public String autodetectedParams(String payload, @BeanParam OtherBeanParam otherBeanParam) {
            return payload;
        }
    }
}
