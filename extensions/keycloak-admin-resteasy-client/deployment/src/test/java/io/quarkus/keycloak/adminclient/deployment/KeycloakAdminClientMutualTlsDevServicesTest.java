package io.quarkus.keycloak.adminclient.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = {
        Format.PKCS12, Format.PEM }, client = true))
public class KeycloakAdminClientMutualTlsDevServicesTest {

    @RegisterExtension
    final static QuarkusDevModeTest app = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MtlsResource.class)
                    .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-server-ca.crt"), "server-ca.crt")
                    .addAsResource(new File("target/certs/mtls-test-client-keystore.p12"), "client-keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-client-truststore.p12"), "client-truststore.p12")
                    .addAsResource("app-mtls-config.properties", "application.properties"));

    @Test
    public void testCreateRealm() {
        // create realm
        RestAssured.given().post("/api/mtls").then().statusCode(204);
        // test realm created
        RestAssured.given().get("/api/mtls/Ron").then().statusCode(200).body(Matchers.is("Weasley"));
    }

    @Path("/api/mtls")
    public static class MtlsResource {

        @Inject
        Keycloak keycloak;

        @POST
        public void createMtlsRealm() {
            var realm = new RealmRepresentation();
            realm.setRealm("mtls");
            realm.setEnabled(true);
            RolesRepresentation roles = new RolesRepresentation();
            List<RoleRepresentation> realmRoles = new ArrayList<>();
            roles.setRealm(realmRoles);
            realm.setRoles(roles);
            realm.getRoles().getRealm().add(new RoleRepresentation("Ron", "Weasley", false));
            keycloak.realms().create(realm);
        }

        @Path("{roleName}")
        @GET
        public String getRoleDescription(@PathParam("roleName") String roleName) {
            return keycloak.realm("mtls").roles().get(roleName).toRepresentation().getDescription();
        }

    }
}
