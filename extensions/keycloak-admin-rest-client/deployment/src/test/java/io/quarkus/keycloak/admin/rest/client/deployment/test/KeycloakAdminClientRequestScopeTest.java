package io.quarkus.keycloak.admin.rest.client.deployment.test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.admin.client.Keycloak;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class KeycloakAdminClientRequestScopeTest {

    @RegisterExtension
    final static QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(AdminResource.class)
                    .addAsResource(new StringAsset("""
                            quarkus.keycloak.devservices.enabled=false
                            quarkus.keycloak.admin-client.server-url=http://localhost:1
                            """), "application.properties"));

    @Test
    public void testEachRequestGetsItsOwnClient() {
        String first = RestAssured.get("/api/admin/client").then().statusCode(200).extract().asString();
        String second = RestAssured.get("/api/admin/client").then().statusCode(200).extract().asString();
        assertNotEquals(first, second);
        // the test never closes a client, the container does when the request scope ends
        RestAssured.get("/api/admin/closed").then().statusCode(200).body(is("[true, true]"));
    }

    @Path("/api/admin")
    public static class AdminResource {

        static final List<Keycloak> CLIENTS = new CopyOnWriteArrayList<>();

        @Inject
        Keycloak keycloak;

        @GET
        @Path("/client")
        public String client() {
            Keycloak instance = ClientProxy.unwrap(keycloak);
            assertFalse(instance.isClosed());
            CLIENTS.add(instance);
            return String.valueOf(System.identityHashCode(instance));
        }

        @GET
        @Path("/closed")
        public String closed() {
            return CLIENTS.stream().map(Keycloak::isClosed).toList().toString();
        }
    }
}
