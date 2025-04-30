package io.quarkus.resteasy.test.security;

import static io.quarkus.vertx.http.runtime.attribute.RemoteUserAttribute.REMOTE_USER_SHORT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RemoteUserHttpAccessLogTest {

    @RegisterExtension
    public static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(RolesAllowedResource.class, SecurityOverrideFilter.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class)
                    .add(new StringAsset("quarkus.http.access-log.enabled=true\n" +
                            "quarkus.http.access-log.pattern=%h %t " + REMOTE_USER_SHORT), "application.properties"))
            .setLogRecordPredicate(logRecord -> logRecord.getLevel().equals(Level.INFO)
                    && logRecord.getLoggerName().equals("io.quarkus.http.access-log"))
            .assertLogRecords(logRecords -> {
                var accessLogRecords = logRecords
                        .stream()
                        .map(LogRecord::getMessage)
                        .collect(Collectors.toList());
                assertTrue(accessLogRecords.stream().anyMatch(msg -> msg.endsWith("admin")));
                assertFalse(accessLogRecords.stream().anyMatch(msg -> msg.endsWith("user")));
                assertTrue(accessLogRecords.stream().anyMatch(msg -> msg.endsWith("Charlie")));
            });

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testAuthRemoteUserLogged() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .get("/roles")
                .then()
                .statusCode(200)
                .body(Matchers.is("default"));
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .get("/roles")
                .then()
                .statusCode(200)
                .body(Matchers.is("default"));
    }

    @Provider
    @PreMatching
    public static class SecurityOverrideFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getSecurityContext().getUserPrincipal().getName().equals("user")) {
                requestContext.setSecurityContext(new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return new Principal() {
                            @Override
                            public String getName() {
                                return "Charlie";
                            }
                        };
                    }

                    @Override
                    public boolean isUserInRole(String r) {
                        return "user".equals(r);
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "basic";
                    }
                });
            }

        }
    }
}
