package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.quarkus.vertx.http.runtime.security.AbstractPathMatchingHttpSecurityPolicy;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

@QuarkusTest
@TestProfile(AnnotationBasedTenantTest.NoProactiveAuthTestProfile.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
public class AnnotationBasedTenantTest {
    public static class NoProactiveAuthTestProfile implements QuarkusTestProfile {
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(Map.entry("quarkus.http.auth.proactive", "false"),
                    Map.entry("quarkus.oidc.hr.authentication.user-info-required", "false"),
                    Map.entry("quarkus.oidc.hr.auth-server-url", "http://localhost:8180/auth/realms/quarkus2/"),
                    Map.entry("quarkus.oidc.hr.client-id", "quarkus-app"),
                    Map.entry("quarkus.oidc.hr.credentials.secret", "secret"),
                    Map.entry("quarkus.oidc.hr.tenant-paths", "/api/tenant-echo/http-security-policy-applies-all-same"),
                    Map.entry("quarkus.oidc.hr.token.audience", "http://hr.service"),
                    Map.entry("quarkus.oidc.hr.follow-redirects", "false"),
                    Map.entry("quarkus.http.auth.policy.roles1.roles-allowed", "role1"),
                    Map.entry("quarkus.http.auth.policy.roles2.roles-allowed", "role2"),
                    Map.entry("quarkus.http.auth.policy.roles3.roles-allowed", "role3,role2"),
                    Map.entry("quarkus.http.auth.policy.roles3.permissions.role3", "get-tenant"),
                    Map.entry("quarkus.http.auth.roles-mapping.role4", "role3"),
                    Map.entry("quarkus.http.auth.permission.jax-rs1.paths", "/api/tenant-echo2/hr-jax-rs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.jax-rs1.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.jax-rs1.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.jax-rs2.paths", "/api/tenant-echo/hr-jax-rs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.jax-rs2.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.jax-rs2.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.classic.paths", "/api/tenant-echo2/hr-classic-perm-check"),
                    Map.entry("quarkus.http.auth.permission.classic.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.combined-part1.paths",
                            "/api/tenant-echo2/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part1.policy", "roles2"),
                    Map.entry("quarkus.http.auth.permission.combined-part2.paths",
                            "/api/tenant-echo2/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part2.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.combined-part2.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.combined-part3.paths",
                            "/api/tenant-echo/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part3.policy", "roles2"),
                    Map.entry("quarkus.http.auth.permission.combined-part4.paths",
                            "/api/tenant-echo/hr-classic-and-jaxrs-perm-check"),
                    Map.entry("quarkus.http.auth.permission.combined-part4.policy", "roles1"),
                    Map.entry("quarkus.http.auth.permission.combined-part4.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.identity-augmentation.paths",
                            "/api/tenant-echo/hr-identity-augmentation"),
                    Map.entry("quarkus.http.auth.permission.identity-augmentation.policy", "roles3"),
                    Map.entry("quarkus.http.auth.permission.identity-augmentation.applies-to", "JAXRS"),
                    Map.entry("quarkus.http.auth.permission.tenant-annotation-applies-all.paths",
                            "/api/tenant-echo/http-security-policy-applies-all-diff,/api/tenant-echo/http-security-policy-applies-all-same"),
                    Map.entry("quarkus.http.auth.permission.tenant-annotation-applies-all.policy", "admin-role"),
                    Map.entry("quarkus.http.auth.policy.admin-role.roles-allowed", "admin"),
                    Map.entry("quarkus.oidc.redirect-loop.auth-server-url", "http://localhost:8180/auth/realms/quarkus3/"),
                    Map.entry("quarkus.oidc.redirect-loop.follow-redirects", "false"),
                    Map.entry("quarkus.oidc.redirect-loop2.auth-server-url", "http://localhost:8180/auth/realms/quarkus4/"),
                    Map.entry("quarkus.oidc.redirect-loop2.follow-redirects", "false"),
                    Map.entry("quarkus.oidc.redirect-loop3.auth-server-url", "http://localhost:8180/auth/realms/quarkus5/"),
                    Map.entry("quarkus.oidc.redirect-loop3.follow-redirects", "false"));
        }

        @Override
        public String getConfigProfile() {
            return "jax-rs-http-perms-test";
        }
    }

    @Inject
    Vertx vertx;

    @TestHTTPResource("/ws/tenant-annotation/permissions-allowed")
    URI websocketPermissionsAllowedTenantAnnotation;

    @TestHTTPResource("/ws/tenant-annotation/hr-tenant")
    URI websocketHrTenantAnnotation;

    @TestHTTPResource("/ws/tenant-annotation/no-annotation")
    URI websocketNoTenantAnnotation;

    @Test
    public void testClassLevelAnnotation() {
        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo")
                    .then().statusCode(401);

            token = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api-redirect-loop")
                    .then().statusCode(500);

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api-redirect-loop2")
                    .then().statusCode(500);

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api-redirect-loop3")
                    .then().statusCode(500);

        } finally {
            server.stop();
        }
    }

    @Test
    public void testMethodLevelAnnotation() {
        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // ANNOTATED ENDPOINT
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr")
                    .then().statusCode(401);

            token = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

            // UNANNOTATED ENDPOINT
            token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/default")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=Default, static.tenant.id=null, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=null")));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsHttpSecurityPolicyNoRbac() {
        // there is one HTTP permission check for this path, and it is executed after @Tenant has chosen right tenant
        // no RBAC annotation is applied
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-jax-rs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-jax-rs-perm-check")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

            token = getTokenWithRole("wrong-role");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-jax-rs-perm-check")
                    .then().statusCode(403);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsHttpSecurityPolicyWithRbac() {
        // there is one HTTP permission check for this path, and it is executed after @Tenant has chosen right tenant
        // also the endpoint is secured with @Authenticated
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-jax-rs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-jax-rs-perm-check")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));

            token = getTokenWithRole("wrong-role");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-jax-rs-perm-check")
                    .then().statusCode(403);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testClassicHttpSecurityPolicyWithRbac() {
        // there is one HTTP permission check for this path, and it is executed before @Tenant comes into action
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-perm-check")
                    .then().statusCode(403);

            // Static tenant id is wrong as authentication happened before tenant were selected via @Tenant
            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("wrong-role");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-perm-check")
                    .then().statusCode(403);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsAndClassicHttpSecurityPolicyNoRbac() {
        // there are 2 HTTP Permission checks for this path, one happens before tenant selection, one happens "after" it
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part1" as "role2" is missing
            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part2" as "role1" is missing
            token = getTokenWithRole("role2");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);

            // roles allowed security check (created for @RolesAllowed) fails over missing role "role3"
            token = getTokenWithRole("role2", "role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role3", "role2", "role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsAndClassicHttpSecurityPolicyWithRbac() {
        // there are 2 HTTP Permission checks for this path, one happens before tenant selection, one happens "after" it
        // also @Authenticated is applied
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part1" as "role2" is missing
            token = getTokenWithRole("role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(403);

            // permission check "combined-part2" as "role1" is missing
            token = getTokenWithRole("role2");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);

            token = getTokenWithRole("role2", "role1");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr-classic-and-jaxrs-perm-check")
                    .then().statusCode(401);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testJaxRsIdentityAugmentation() {
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // pass JAX-RS permission check but missing permission
            String token = getTokenWithRole("role2");
            AuthEventObserver.clearEvents();
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-identity-augmentation")
                    .then().statusCode(403);
            Assertions.assertEquals(1, AuthEventObserver.getAuthorizationFailureEvents().size());

            token = getTokenWithRole("role3");
            AuthEventObserver.clearEvents();
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-identity-augmentation")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));
            // expect one JAX-RS HTTP Permission check and one Security check for the PermissionsAllowed annotation
            Assertions.assertEquals(2, AuthEventObserver.getAuthorizationSuccessEvents().size());
            AuthorizationSuccessEvent successEvent = AuthEventObserver
                    .getAuthorizationSuccessEvents()
                    .stream()
                    .filter(ev -> AbstractPathMatchingHttpSecurityPolicy.class.getName()
                            .equals(ev.getEventProperties().get(AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT)))
                    .findFirst()
                    .orElseThrow();

            token = getTokenWithRole("role4");
            AuthEventObserver.clearEvents();
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/hr-identity-augmentation")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice, "
                            + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "=hr")));
            // quarkus.http.auth.roles-mapping mapped role4 to role3, check that role3 is already present inside authN event
            Assertions.assertEquals(1, AuthEventObserver.getAuthenticationSuccessEvents().size());
            SecurityIdentity identity = AuthEventObserver.getAuthenticationSuccessEvents().get(0)
                    .getSecurityIdentity();
            Assertions.assertNotNull(identity);
            Assertions.assertTrue(identity.hasRole("role3"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testPolicyAppliedBeforeTenantAnnotationMatched() {
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // policy applied before @Tenant annotation has been matched and different tenant has been used for auth
            // than the one that @Tenant annotation selects
            var token = getNonHrTenantAccessToken(Set.of("admin"));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/http-security-policy-applies-all-diff")
                    .then().statusCode(401);

            // policy applied before @Tenant annotation has been matched and different tenant has been used for auth
            // than the one that @Tenant annotation selects
            token = getTokenWithRole("admin");
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo/http-security-policy-applies-all-same")
                    .then().statusCode(200)
                    .body(Matchers
                            .equalTo("tenant-id=hr, static.tenant.id=hr, name=alice, tenant-id-set-by-annotation=null"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testWebSocketsHttpUpgradeTenantAnnotation() throws InterruptedException, ExecutionException, TimeoutException {
        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // @Authenticated and @Tenant("hr")
            // correct HR tenant -> pass
            String correctToken = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");
            callWebSocketEndpoint(websocketHrTenantAnnotation, "hr-tenant", correctToken, false);
            // no token -> fail
            RuntimeException ce = assertThrows(RuntimeException.class,
                    () -> callWebSocketEndpoint(websocketHrTenantAnnotation, "hr-tenant", null, true));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
            // wrong audience -> fail
            String tokenWithWrongAudience = OidcWiremockTestResource.getAccessToken("alice",
                    new HashSet<>(Arrays.asList("user", "admin")));
            ce = assertThrows(RuntimeException.class,
                    () -> callWebSocketEndpoint(websocketHrTenantAnnotation, "hr-tenant", tokenWithWrongAudience, true));
            root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
            // now use same token with wrong audience to connect with @Authenticated endpoint without @Tenant("hr")
            callWebSocketEndpoint(websocketNoTenantAnnotation, "no-annotation", correctToken, false);

            // @PermissionsAllowed("bob") @Tenant("hr")
            // bob -> pass
            String bobToken = Jwt.preferredUserName("bob")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");
            callWebSocketEndpoint(websocketPermissionsAllowedTenantAnnotation, "permissions-allowed", bobToken, false);
            // alice -> authorization failure
            String aliceToken = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");
            ce = assertThrows(RuntimeException.class,
                    () -> callWebSocketEndpoint(websocketPermissionsAllowedTenantAnnotation, "permissions-allowed", aliceToken,
                            true));
            root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("403"), root.getMessage());
            String aliceWithWrongAudience = OidcWiremockTestResource.getAccessToken("alice",
                    new HashSet<>(Arrays.asList("user", "admin")));
            ce = assertThrows(RuntimeException.class,
                    () -> callWebSocketEndpoint(websocketPermissionsAllowedTenantAnnotation, "permissions-allowed",
                            aliceWithWrongAudience, true));
            root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
        } finally {
            server.stop();
        }
    }

    private void callWebSocketEndpoint(URI uri, String expectedResponsePrefix, String token, boolean expectFailure)
            throws ExecutionException, InterruptedException, TimeoutException {
        callWebSocketEndpoint(uri, expectedResponsePrefix, token, expectFailure, vertx);
    }

    static void callWebSocketEndpoint(URI uri, String expectedResponsePrefix, String token, boolean expectFailure,
            Vertx vertx) throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(2);
        List<String> messages = new CopyOnWriteArrayList<>();
        AtomicReference<WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(uri.getHost());
        options.setPort(uri.getPort());
        options.setURI(uri.getPath());
        if (token != null) {
            options.addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token);
        }
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        try {
            client
                    .connect(options)
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            WebSocket ws = r.result();
                            ws.textMessageHandler(msg -> {
                                messages.add(msg);
                                messagesLatch.countDown();
                            });
                            // We will use this socket to write a message later on
                            ws1.set(ws);
                            connectedLatch.countDown();
                        } else {
                            throwable.set(r.cause());
                        }
                    });
            if (expectFailure) {
                Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> throwable.get() != null);
                throw new RuntimeException(throwable.get());
            } else {
                Assertions.assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
                ws1.get().writeTextMessage("hello");
                Assertions.assertTrue(messagesLatch.await(5, TimeUnit.SECONDS), "Messages: " + messages);
                Assertions.assertEquals(2, messages.size(), "Messages: " + messages);
                Assertions.assertEquals("ready", messages.get(0));
                Assertions.assertEquals(expectedResponsePrefix + " echo: " + "hello", messages.get(1));
            }
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    private static String getTokenWithRole(String... roles) {
        return Jwt.preferredUserName("alice")
                .groups(Set.of(roles))
                .audience("http://hr.service")
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
    }

    private String getNonHrTenantAccessToken(Set<String> groups) {
        return Jwt.preferredUserName("alice")
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws()
                .keyId("1")
                .sign();
    }
}
