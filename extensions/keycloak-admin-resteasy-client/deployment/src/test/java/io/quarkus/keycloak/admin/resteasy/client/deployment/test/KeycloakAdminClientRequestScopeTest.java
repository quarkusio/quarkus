package io.quarkus.keycloak.admin.resteasy.client.deployment.test;

import static org.hamcrest.Matchers.is;
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
        RestAssured.get("/api/admin/closed").then().statusCode(200).body(is("2"));
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
            CLIENTS.add(instance);
            return String.valueOf(System.identityHashCode(instance));
        }

        @GET
        @Path("/closed")
        public long closed() {
            return CLIENTS.stream().filter(Keycloak::isClosed).count();
        }
    }
}
