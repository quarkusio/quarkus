package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.quarkus.resteasy.reactive.jackson.EnableSecureSerialization;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public class DisableSecureSerializationTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class));

    @Test
    public void testDisablingOfSecureSerialization() {
        request("disabled", "user").body("secretField", Matchers.is("secret"));
        request("disabled", "admin").body("secretField", Matchers.is("secret"));
        request("enabled", "user").body("secretField", Matchers.nullValue());
        request("enabled", "admin").body("secretField", Matchers.is("secret"));
    }

    private static ValidatableResponse request(String subPath, String user) {
        TestIdentityController.resetRoles().add(user, user, user);
        return RestAssured
                .with()
                .auth().preemptive().basic(user, user)
                .get("/test/" + subPath)
                .then()
                .statusCode(200)
                .body("publicField", Matchers.is("public"));
    }

    @DisableSecureSerialization
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("test")
    public static class GreetingsResource {

        @Path("disabled")
        @GET
        public Dto disabled() {
            return Dto.createDto();
        }

        @EnableSecureSerialization
        @Path("enabled")
        @GET
        public Dto enabled() {
            return Dto.createDto();
        }
    }

    public static class Dto {

        public Dto(String secretField, String publicField) {
            this.secretField = secretField;
            this.publicField = publicField;
        }

        @SecureField(rolesAllowed = "admin")
        private final String secretField;

        private final String publicField;

        public String getSecretField() {
            return secretField;
        }

        public String getPublicField() {
            return publicField;
        }

        private static Dto createDto() {
            return new Dto("secret", "public");
        }
    }
}
