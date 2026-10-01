package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import java.net.URL;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ManagementInterface;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.restassured.RestAssured;

/**
 * Tests that an HTTP Security policy path which does not start with a forward slash is resolved
 * relatively to the {@code quarkus.management.root-path} of the management interface.
 */
public class ManagementPathWithoutForwardSlashAndRootPathTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.management.enabled=true\n" +
            "quarkus.management.auth.enabled=true\n" +
            "quarkus.management.root-path=/root\n" +
            "quarkus.management.auth.permission.authenticated.paths=service\n" +
            "quarkus.management.auth.permission.authenticated.policy=authenticated\n";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, ManagementPathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @TestHTTPResource(value = "/service", management = true)
    URL service;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    @Test
    public void testAnonymousServicePath() {

        RestAssured
                .given()
                .when()
                .get(service)
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testAuthenticatedServicePath() {

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get(service)
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/root/service"));
    }

    public static class ManagementPathHandler {

        void setup(@Observes ManagementInterface mi) {
            mi.router().get("/root/service").handler(event -> {
                QuarkusHttpUser user = (QuarkusHttpUser) event.user();
                StringBuilder ret = new StringBuilder();
                if (user != null) {
                    ret.append(user.getSecurityIdentity().getPrincipal().getName());
                }
                ret.append(":");
                ret.append(event.normalizedPath());
                event.response().end(ret.toString());
            });
        }
    }
}
