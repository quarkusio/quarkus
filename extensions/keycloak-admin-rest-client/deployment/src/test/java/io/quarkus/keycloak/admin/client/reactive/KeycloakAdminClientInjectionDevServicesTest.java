package io.quarkus.keycloak.admin.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class KeycloakAdminClientInjectionDevServicesTest {

    @RegisterExtension
    final static QuarkusDevModeTest app = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(AdminResource.class)
                    .addAsResource("app-dev-mode-config.properties", "application.properties"));

    @Test
    public void testGetRoles() {
        // use 'password' grant type
        final Response getRolesReq = RestAssured.given().get("/api/admin/roles");
        assertEquals(200, getRolesReq.statusCode());
        final List<RoleRepresentation> roles = getRolesReq.jsonPath().getList(".", RoleRepresentation.class);
        assertNotNull(roles);
        // assert there are roles admin and user (among others)
        assertTrue(roles.stream().anyMatch(rr -> "user".equals(rr.getName())));
        assertTrue(roles.stream().anyMatch(rr -> "admin".equals(rr.getName())));
    }

    @Path("/api/admin")
    public static class AdminResource {

        @Inject
        Keycloak keycloak;

        @GET
        @Path("/roles")
        public List<RoleRepresentation> getRoles() {
            return keycloak.realm("quarkus").roles().list();
        }

    }
}
