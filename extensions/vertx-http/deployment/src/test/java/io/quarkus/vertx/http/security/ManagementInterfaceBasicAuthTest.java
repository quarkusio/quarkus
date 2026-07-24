package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import java.net.URL;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
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
import io.quarkus.vertx.http.runtime.security.ManagementInterfaceHttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.restassured.RestAssured;
import io.vertx.ext.web.RoutingContext;

/**
 * Tests that basic authentication is enabled for the management interface when no other
 * mechanism is available.
 */
public class ManagementInterfaceBasicAuthTest {

    private static final String CHECK_RESULT_HEADER = "check-result-header-name";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                            ManagementPathHandler.class)
                    .addAsResource(new StringAsset("""
                            quarkus.management.enabled=true
                            quarkus.management.auth.enabled=true
                            quarkus.management.auth.policy.r1.roles-allowed=admin
                            quarkus.management.auth.permission.roles1.paths=/q/metrics
                            quarkus.management.auth.permission.roles1.policy=r1
                            """), "application.properties");
        }
    });

    @TestHTTPResource(value = "/metrics", management = true)
    URL metrics;

    @TestHTTPResource(value = "/traces", management = true)
    URL traces;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
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
                .body(equalTo("admin:" + metrics.getPath()))
                .header(CHECK_RESULT_HEADER, Matchers.is("true"));

    }

    @Test
    public void testBasicAuthFailureWithoutPassword() {
        RestAssured
                .given()
                .redirects().follow(false)
                .get(metrics)
                .then()
                .assertThat()
                .statusCode(401)
                .header(CHECK_RESULT_HEADER, Matchers.is("false"));

    }

    @Test
    public void testBasicAuthFailureInsufficientRoles() {
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .redirects().follow(false)
                .get(metrics)
                .then()
                .assertThat()
                .statusCode(403)
                .header(CHECK_RESULT_HEADER, Matchers.is("false"));

    }

    @Test
    public void testBasicAuthFailureWithoutPasswordWithMatrix() {
        RestAssured
                .given()
                .redirects().follow(false)
                .get(metrics.toString() + ";a=a1")
                .then()
                .assertThat()
                .statusCode(404); // instead of 401

    }

    @Test
    public void testBasicAuthFailureWrongPassword() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "wrongpassword")
                .redirects().follow(false)
                .get(metrics)
                .then()
                .assertThat()
                .statusCode(401);

    }

    @Test
    public void testPermissionEvaluationReturnsNullWhenNoAuthorization() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .get(traces)
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:" + traces.getPath()))
                .header(CHECK_RESULT_HEADER, Matchers.nullValue());
    }

    @Test
    public void testBasicAuthFailureWrongPasswordWithMatrix() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "wrongpassword")
                .redirects().follow(false)
                .get(metrics + ";a=a1")
                .then()
                .assertThat()
                .statusCode(401);

    }

    public static class ManagementPathHandler {

        void setup(@Observes ManagementInterface mi, ManagementInterfaceHttpAuthorizer managementAuthorizer) {
            mi.router().route().order(-1 * (SecurityHandlerPriorities.AUTHENTICATION - 10))
                    .handler(event -> managementAuthorizer
                            .evaluatePermission(event).subscribe().with(
                                    checkResult -> {
                                        if (checkResult != null) {
                                            event.response().putHeader(CHECK_RESULT_HEADER,
                                                    Boolean.toString(checkResult.isPermitted()));
                                        }
                                        event.next();
                                    },
                                    throwable -> event.fail(500, throwable)));
            mi.router().get("/q/metrics").handler(ManagementInterfaceBasicAuthTest::respond);
            mi.router().get("/q/traces").handler(ManagementInterfaceBasicAuthTest::respond);
        }
    }

    private static void respond(RoutingContext event) {
        QuarkusHttpUser user = (QuarkusHttpUser) event.user();
        StringBuilder ret = new StringBuilder();
        if (user != null) {
            ret.append(user.getSecurityIdentity().getPrincipal().getName());
        }
        ret.append(":");
        ret.append(event.normalizedPath());
        event.response().end(ret.toString());
    }
}
