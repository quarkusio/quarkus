package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.AnnotationBasedTenantTest.callWebSocketEndpoint;
import static org.hamcrest.Matchers.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.Tenant;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@QuarkusTest
@QuarkusTestResource(CustomOidcWiremockTestResource.class)
@TestProfile(BearerTokenStepUpAuthenticationTest.LazyAuthenticationTestProfile.class)
public class BearerTokenStepUpAuthenticationTest {

    @TestHTTPResource("/ws/tenant-annotation/bearer-step-up-auth")
    URI websocketAuthCtxUri;

    @Inject
    Vertx vertx;

    @Test
    public void testMethodLevelAuthCtxNoRbac() {
        // anonymous, no RBAC annotations on endpoint but acr required -> fail
        RestAssured.given()
                .when().get("/step-up-auth/method-level/no-rbac-annotation")
                .then().statusCode(401);
        // no acr -> fail
        stepUpMethodLevelRequest(Set.of(), Set.of(), "no-rbac-annotation").statusCode(401);
        // wrong single acr -> fail
        stepUpMethodLevelRequest(Set.of(), Set.of("3"), "no-rbac-annotation").statusCode(401);
        // wrong multiple acr -> fail
        stepUpMethodLevelRequest(Set.of(), Set.of("3", "4"), "no-rbac-annotation").statusCode(401);
        // correct acr -> pass
        stepUpMethodLevelRequest(Set.of(), Set.of("1"), "no-rbac-annotation").statusCode(200).body(is("no-rbac-annotation"));
    }

    @Test
    public void testClassLevelAuthCtxNoRbac() {
        // anonymous, no RBAC annotations on endpoint but acr required -> fail
        RestAssured.given()
                .when().get("/step-up-auth/class-level/no-rbac-annotation")
                .then().statusCode(401);
        // no acr -> fail
        stepUpClassLevelRequest(Set.of(), Set.of(), "no-rbac-annotation").statusCode(401);
        // wrong single acr -> fail
        stepUpClassLevelRequest(Set.of(), Set.of("3"), "no-rbac-annotation").statusCode(401);
        // wrong multiple acr -> fail
        stepUpClassLevelRequest(Set.of(), Set.of("3", "4"), "no-rbac-annotation").statusCode(401);
        // correct acr -> pass
        stepUpClassLevelRequest(Set.of(), Set.of("2"), "no-rbac-annotation").statusCode(200).body(is("no-rbac-annotation"));
    }

    @Test
    public void testMethodLevelAuthCtxRolesAllowed() {
        // no acr -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of(), "admin-role").statusCode(401);
        // wrong single acr -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("1"), "admin-role").statusCode(401);
        // wrong multiple acr -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("1", "4"), "admin-role").statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("3"));
        // correct acr & wrong role -> fail
        stepUpMethodLevelRequest(Set.of("user"), Set.of("3"), "admin-role").statusCode(403);
        // correct acr & correct role -> pass
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("3"), "admin-role").statusCode(200).body(is("admin-role"));
    }

    @Test
    public void testMethodLevelAuthTenantAnnotationSelection() {
        // wrong acr & correct tenant -> fail
        RestAssured.given()
                .auth().oauth2(getAccessToken(Set.of(), Set.of("3")))
                .when().get("/tenant-ann-step-up-auth/bearer-step-up-auth-1")
                .then()
                .statusCode(401);
        // correct acr & tenant -> pass
        RestAssured.given()
                .auth().oauth2(getAccessToken(Set.of(), Set.of("6")))
                .when().get("/tenant-ann-step-up-auth/bearer-step-up-auth-1")
                .then()
                .statusCode(200)
                .body(is("bearer-step-up-auth"));
        // correct acr & second tenant -> pass
        RestAssured.given()
                .auth().oauth2(getAccessToken(Set.of(), Set.of("6")))
                .when().get("/tenant-ann-step-up-auth/bearer-step-up-auth-2")
                .then()
                .statusCode(200)
                .body(is("bearer-step-up-auth-2"));
    }

    @Test
    public void testClassLevelAuthCtxRolesAllowed() {
        // no acr -> fail
        stepUpClassLevelRequest(Set.of("admin"), Set.of(), "admin-role").statusCode(401);
        // wrong single acr -> fail
        stepUpClassLevelRequest(Set.of("admin"), Set.of("1"), "admin-role").statusCode(401);
        // wrong multiple acr -> fail
        stepUpClassLevelRequest(Set.of("admin"), Set.of("1", "4"), "admin-role").statusCode(401);
        // correct acr & wrong role -> fail
        stepUpClassLevelRequest(Set.of("user"), Set.of("2"), "admin-role").statusCode(403);
        // correct acr & correct role -> pass
        stepUpClassLevelRequest(Set.of("admin"), Set.of("2"), "admin-role").statusCode(200).body(is("admin-role"));
    }

    @Test
    public void testMethodLevelMultipleAcrsRequired() {
        // no acr -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of(), "multiple-acr-required").statusCode(401);
        // wrong single acr -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("4"), "multiple-acr-required").statusCode(401);
        // wrong multiple acr -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("4", "5"), "multiple-acr-required").statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("1"))
                .header("www-authenticate", containsString("2"))
                .header("www-authenticate", containsString("3"));
        // one wrong, one correct acr -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("1", "4"), "multiple-acr-required").statusCode(401);
        // one wrong, two correct acrs -> fail
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("1", "2", "4"), "multiple-acr-required").statusCode(401);
        // correct acrs -> pass
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("1", "2", "3"), "multiple-acr-required").statusCode(200)
                .body(is("multiple-acr-required"));
        // correct acrs & an irrelevant extra acr -> pass
        stepUpMethodLevelRequest(Set.of("admin"), Set.of("1", "2", "3", "4"), "multiple-acr-required").statusCode(200)
                .body(is("multiple-acr-required"));
    }

    @Test
    public void testWebSocketsClassLevelAuthContextAnnotation()
            throws ExecutionException, InterruptedException, TimeoutException {
        // wrong acr -> fail
        String wrongToken = getAccessToken(Set.of(), Set.of("3"));
        Assertions.assertThrows(RuntimeException.class,
                () -> callWebSocketEndpoint(websocketAuthCtxUri, "bearer-step-up-auth", wrongToken, true, vertx));
        // correct acr -> pass
        String correctToken = getAccessToken(Set.of(), Set.of("7"));
        callWebSocketEndpoint(websocketAuthCtxUri, "bearer-step-up-auth", correctToken, false, vertx);
    }

    private static ValidatableResponse stepUpMethodLevelRequest(Set<String> roles, Set<String> acrValues, String path) {
        return stepUpRequest(roles, acrValues, "method", path);
    }

    private static ValidatableResponse stepUpClassLevelRequest(Set<String> roles, Set<String> acrValues, String path) {
        return stepUpRequest(roles, acrValues, "class", path);
    }

    private static ValidatableResponse stepUpRequest(Set<String> roles, Set<String> acrValues, String level, String path) {
        return RestAssured.given()
                .auth().oauth2(getAccessToken(roles, acrValues))
                .when().get("/step-up-auth/" + level + "-level/" + path)
                .then();
    }

    private static String getAccessToken(Set<String> roles, Set<String> acrValues) {
        return Jwt.preferredUserName("Pete")
                .groups(roles)
                .claim(Claims.acr, acrValues)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .jws().algorithm(SignatureAlgorithm.PS256)
                .sign();
    }

    public static final class LazyAuthenticationTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            var props = new HashMap<String, String>();
            props.put("quarkus.http.auth.proactive", "false");
            props.put("quarkus.oidc.bearer-step-up-auth.auth-server-url",
                    "${keycloak.url:replaced-by-test-resource}/realms/quarkus/");
            props.put("quarkus.oidc.bearer-step-up-auth.client-id", "quarkus-app");
            props.put("quarkus.oidc.bearer-step-up-auth.credentials.secret", "secret");
            props.put("quarkus.oidc.bearer-step-up-auth.token.signature-algorithm", "PS256");
            props.put("quarkus.oidc.bearer-step-up-auth.tenant-paths", "/step-up-auth/*");
            props.put("quarkus.oidc.bearer-step-up-auth-2.auth-server-url",
                    "${keycloak.url:replaced-by-test-resource}/realms/quarkus/");
            props.put("quarkus.oidc.bearer-step-up-auth-2.client-id", "quarkus-app");
            props.put("quarkus.oidc.bearer-step-up-auth-2.credentials.secret", "secret");
            return Map.copyOf(props);
        }
    }

    @Path("/step-up-auth/method-level")
    public static class StepUpAuthMethodLevelResource {

        @GET
        @AuthenticationContext("1")
        @Path("no-rbac-annotation")
        public String noRbacAnnotationMethodLevel() {
            return "no-rbac-annotation";
        }

        @GET
        @AuthenticationContext("3")
        @RolesAllowed("admin")
        @Path("admin-role")
        public String adminRoleMethodLevel() {
            return "admin-role";
        }

        @GET
        @AuthenticationContext({ "1", "2", "3" })
        @Path("multiple-acr-required")
        public String multipleAcrRequiredMethodLevel() {
            return "multiple-acr-required";
        }

    }

    @Path("/tenant-ann-step-up-auth")
    public static class TenantAnnotationStepUpAuthResource {

        @Inject
        RoutingContext routingContext;

        @Tenant("bearer-step-up-auth")
        @AuthenticationContext("6")
        @GET
        @Path("/bearer-step-up-auth-1")
        public String firstTenantSelectedMethodLevel() {
            return getTenantId();
        }

        @Tenant("bearer-step-up-auth-2")
        @AuthenticationContext("6")
        @GET
        @Path("/bearer-step-up-auth-2")
        public String secondTenantSelectedMethodLevel() {
            return getTenantId();
        }

        private String getTenantId() {
            OidcTenantConfig tenantConfig = routingContext.get(OidcTenantConfig.class.getName());
            return tenantConfig.tenantId().get();
        }

    }

    @AuthenticationContext("2")
    @Path("/step-up-auth/class-level")
    public static class StepUpAuthClassLevelResource {

        @GET
        @Path("no-rbac-annotation")
        public String noRbacAnnotationMethodLevel() {
            return "no-rbac-annotation";
        }

        @GET
        @RolesAllowed("admin")
        @Path("admin-role")
        public String adminRoleMethodLevel() {
            return "admin-role";
        }

    }

    @AuthenticationContext("7")
    @Tenant("bearer-step-up-auth")
    @WebSocket(path = "/ws/tenant-annotation/bearer-step-up-auth")
    public static class WebSocketEndpointWithTenantAnnotationAndAuthCtx {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return "bearer-step-up-auth echo: " + message;
        }

    }
}
