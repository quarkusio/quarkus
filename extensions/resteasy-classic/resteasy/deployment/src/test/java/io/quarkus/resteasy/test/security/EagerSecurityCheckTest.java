package io.quarkus.resteasy.test.security;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

/**
 * Tests that {@link io.quarkus.security.spi.runtime.SecurityCheck}s are executed by Jakarta REST filters.
 */
public class EagerSecurityCheckTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, JsonResource.class,
                            AbstractJsonResource.class, JsonSubResource.class));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testAuthenticated() {
        testPostJson("auth", "admin", true).then().statusCode(400);
        testPostJson("auth", null, true).then().statusCode(401);
        testPostJson("auth", "admin", false).then().statusCode(200);
        testPostJson("auth", null, false).then().statusCode(401);
    }

    @Test
    public void testRolesAllowed() {
        testPostJson("roles", "admin", true).then().statusCode(400);
        testPostJson("roles", "user", true).then().statusCode(403);
        testPostJson("roles", "admin", false).then().statusCode(200);
        testPostJson("roles", "user", false).then().statusCode(403);
    }

    @Test
    public void testRolesAllowedOverriddenMethod() {
        testPostJson("/roles-overridden", "admin", true).then().statusCode(400);
        testPostJson("/roles-overridden", "user", true).then().statusCode(403);
        testPostJson("/roles-overridden", "admin", false).then().statusCode(200);
        testPostJson("/roles-overridden", "user", false).then().statusCode(403);
    }

    @Test
    public void testDenyAll() {
        testPostJson("deny", "admin", true).then().statusCode(403);
        testPostJson("deny", null, true).then().statusCode(401);
        testPostJson("deny", "admin", false).then().statusCode(403);
        testPostJson("deny", null, false).then().statusCode(401);
    }

    @Test
    public void testDenyAllClassLevel() {
        testPostJson("/sub-resource/deny-class-level-annotation", "admin", true).then().statusCode(403);
        testPostJson("/sub-resource/deny-class-level-annotation", null, true).then().statusCode(401);
        testPostJson("/sub-resource/deny-class-level-annotation", "admin", false).then().statusCode(403);
        testPostJson("/sub-resource/deny-class-level-annotation", null, false).then().statusCode(401);
    }

    @Test
    public void testPermitAll() {
        testPostJson("permit", "admin", true).then().statusCode(400);
        testPostJson("permit", null, true).then().statusCode(400);
        testPostJson("permit", "admin", false).then().statusCode(200);
        testPostJson("permit", null, false).then().statusCode(200);
    }

    @Test
    public void testSubResource() {
        testPostJson("/sub-resource/roles", "admin", true).then().statusCode(400);
        testPostJson("/sub-resource/roles", "user", true).then().statusCode(403);
        testPostJson("/sub-resource/roles", "admin", false).then().statusCode(200);
        testPostJson("/sub-resource/roles", "user", false).then().statusCode(403);
    }

    private static Response testPostJson(String path, String username, boolean invalid) {
        var req = RestAssured.given();
        if (username != null) {
            req = req.auth().preemptive().basic(username, username);
        }
        return req
                .contentType(ContentType.JSON)
                .body((invalid ? "}" : "") + "{\"simple\": \"obj\"}").post(path);
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class JsonResource extends AbstractJsonResource {

        @Authenticated
        @Path("/auth")
        @POST
        public JsonObject auth(JsonObject array) {
            return array.put("test", "testval");
        }

        @RolesAllowed("admin")
        @Path("/roles")
        @POST
        public JsonObject roles(JsonObject array) {
            return array.put("test", "testval");
        }

        @PermitAll
        @Path("/permit")
        @POST
        public JsonObject permit(JsonObject array) {
            return array.put("test", "testval");
        }

        @PermitAll
        @Path("/sub-resource")
        public JsonSubResource subResource() {
            return new JsonSubResource();
        }

        @RolesAllowed("admin")
        @Override
        public JsonObject rolesOverridden(JsonObject array) {
            return array.put("test", "testval");
        }
    }

    @DenyAll
    public static class JsonSubResource {
        @RolesAllowed("admin")
        @Path("/roles")
        @POST
        public JsonObject roles(JsonObject array) {
            return array.put("test", "testval");
        }

        @Path("/deny-class-level-annotation")
        @POST
        public JsonObject denyClassLevelAnnotation(JsonObject array) {
            return array.put("test", "testval");
        }
    }

    public static abstract class AbstractJsonResource {
        @DenyAll
        @Path("/deny")
        @POST
        public JsonObject deny(JsonObject array) {
            return array.put("test", "testval");
        }

        @Path("/roles-overridden")
        @POST
        public abstract JsonObject rolesOverridden(JsonObject array);
    }
}
