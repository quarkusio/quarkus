package io.quarkus.resteasy.test.security;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

public class SecurityIdentityAugmentorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.http.auth.basic=true\n"), "application.properties")
                    .addClasses(TestIdentityProvider.class, RolesAllowedResource.class, TestIdentityController.class))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-undertow", Version.getVersion())));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles().add("admin", "admin");
    }

    @Test
    public void testSecurityIdentityAugmentor() {
        RestAssured.given().auth().basic("admin", "admin").get("/roles/admin/security-identity").then().statusCode(200)
                .body(Matchers.is("admin"));
    }

    @ApplicationScoped
    public static class CustomAugmentor implements SecurityIdentityAugmentor {

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
            if (identity.isAnonymous()) {
                return Uni.createFrom().item(identity);
            }
            return context.runBlocking(build(identity));
        }

        @ActivateRequestContext
        Supplier<SecurityIdentity> build(SecurityIdentity identity) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            builder.addRole("admin");
            return builder::build;
        }

    }

}
