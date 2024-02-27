package io.quarkus.resteasy.reactive.server.test.security;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RolesAllowedJaxRsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RolesAllowedResource.class, UserResource.class, RolesAllowedBlockingResource.class,
                            SerializationEntity.class, SerializationRolesResource.class, TestIdentityProvider.class,
                            TestIdentityController.class, UnsecuredSubResource.class, RolesAllowedService.class,
                            RolesAllowedServiceResource.class, UnsecuredResource.class, UnsecuredParentResource.class,
                            UnsecuredResourceInterface.class));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testRolesAllowed() {
        Arrays.asList("/roles", "/roles-blocking").forEach((path) -> {
            RestAssured.get(path).then().statusCode(401);
            RestAssured.given().auth().basic("admin", "admin").get(path).then().statusCode(200);
            RestAssured.given().auth().basic("user", "user").get(path).then().statusCode(200);
            RestAssured.given().auth().basic("admin", "admin").get(path + "/admin").then().statusCode(200);
            RestAssured.given().auth().basic("user", "user").get(path + "/admin").then().statusCode(403);
        });
    }

    @Test
    public void testUser() {
        RestAssured.get("/user").then().body(is(""));
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/user").then().statusCode(200).body(is("admin"));
        RestAssured.given().auth().basic("admin", "admin").get("/user").then().body(is(""));
        RestAssured.given().auth().basic("user", "user").get("/user").then().body(is(""));
        RestAssured.given().auth().preemptive().basic("user", "user").get("/user").then().body(is("user"));
    }

    @Test
    public void testNestedRolesAllowed() {
        // there are 2 different checks in place: user & admin on resource, admin on service
        RestAssured.given().auth().basic("admin", "admin").get("/roles-service/hello").then().statusCode(200)
                .body(is(RolesAllowedService.SERVICE_HELLO));
        RestAssured.given().auth().basic("user", "user").get("/roles-service/hello").then().statusCode(403);
    }

    @Test
    public void testSecurityRunsBeforeValidation() {
        read = false;
        RestAssured.given().body(new SerializationEntity()).post("/roles-validate").then().statusCode(401);
        Assertions.assertFalse(read);
        RestAssured.given().body(new SerializationEntity()).auth().basic("admin", "admin").post("/roles-validate").then()
                .statusCode(200);
        Assertions.assertTrue(read);
        read = false;
        RestAssured.given().body(new SerializationEntity()).auth().basic("user", "user").post("/roles-validate").then()
                .statusCode(200);
        Assertions.assertTrue(read);
        read = false;
        RestAssured.given().body(new SerializationEntity()).auth().basic("admin", "admin").post("/roles-validate/admin").then()
                .statusCode(200);
        Assertions.assertTrue(read);
        read = false;
        RestAssured.given().body(new SerializationEntity()).auth().basic("user", "user").post("/roles-validate/admin").then()
                .statusCode(403);
        Assertions.assertFalse(read);
    }

    @Test
    public void shouldAllowAnnotatedParentEndpoint() {
        // the endpoint has @RolesAllowed, therefore default JAX-RS security should not be applied
        String path = "/unsecured/parent-annotated";
        assertStatus(path, 200, 200, 401);
    }

    @Test
    public void shouldAllowAnnotatedEndpointOnInterface() {
        // the endpoint has @RolesAllowed, therefore default JAX-RS security should not be applied
        String path = "/unsecured/interface-annotated";
        assertStatus(path, 200, 200, 401);
    }

    @Test
    public void shouldDenyUnannotatedOverriddenOnInterfaceImplementor() {
        // @RolesAllowed on interface, however implementor overridden the endpoint method with @Path @GET
        String path = "/unsecured/interface-overridden-declared-on-implementor";
        assertStatus(path, 200, 200, 200);
    }

    @Test
    public void shouldAllowAnnotatedOverriddenEndpointDeclaredOnInterface() {
        // @RolesAllowed on interface and implementor didn't declare endpoint declaring annotations @GET
        String path = "/unsecured/interface-overridden-declared-on-interface";
        assertStatus(path, 200, 200, 401);
        // check that response comes from the overridden method
        given().auth().preemptive()
                .basic("admin", "admin").get(path)
                .then()
                .body(Matchers.is("implementor-response"));
    }

    private void assertStatus(String path, int adminStatus, int userStatus, int anonStatus) {
        given().auth().preemptive()
                .basic("admin", "admin").get(path)
                .then()
                .statusCode(adminStatus);
        given().auth().preemptive()
                .basic("user", "user").get(path)
                .then()
                .statusCode(userStatus);
        when().get(path)
                .then()
                .statusCode(anonStatus);
    }

    static volatile boolean read = false;

    @Provider
    public static class Reader implements MessageBodyReader<SerializationEntity> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public SerializationEntity readFrom(Class<SerializationEntity> type, Type genericType, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            read = true;
            SerializationEntity entity = new SerializationEntity();
            entity.setName("read");
            return entity;
        }
    }
}
