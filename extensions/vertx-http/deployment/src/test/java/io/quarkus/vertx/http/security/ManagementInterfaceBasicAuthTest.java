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
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ManagementInterface;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.restassured.RestAssured;

/**
 * Tests that basic authentication is enabled for the management interface when no other
 * mechanism is available.
 */
public class ManagementInterfaceBasicAuthTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                            ManagementPathHandler.class)
                    .addAsResource(new StringAsset("""
                            quarkus.management.enabled=true
                            quarkus.management.auth.enabled=true
                            quarkus.management.auth.policy.r1.roles-allowed=admin
                            quarkus.management.auth.permission.roles1.paths=/admin
                            quarkus.management.auth.permission.roles1.policy=r1
                            """), "application.properties");
        }
    });

    @TestHTTPResource(value = "/metrics", management = true)
    URL metrics;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testBasicAuthSuccess() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .get(metrics)
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:" + metrics.getPath()));

    }

    @Test
    public void testBasicAuthFailure() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "wrongpassword")
                .redirects().follow(false)
                .get(metrics)
                .then()
                .assertThat()
                .statusCode(401);

    }

    public static class ManagementPathHandler {

        void setup(@Observes ManagementInterface mi) {
            mi.router().get("/q/metrics").handler(event -> {
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
