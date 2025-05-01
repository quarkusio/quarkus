package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.awaitility.Awaitility;
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
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@TestProfile(BearerTokenStepUpAuthenticationTest.StepUpAuthTestProfile.class)
@QuarkusTestResource(KeycloakRealmResourceManager.class)
@QuarkusTest
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
        stepUpMethodLevelRequest(Set.of(), "no-rbac-annotation").statusCode(401);
        // wrong single acr -> fail
        stepUpMethodLevelRequest(Set.of("3"), "no-rbac-annotation").statusCode(401);
        // wrong multiple acr -> fail
        stepUpMethodLevelRequest(Set.of("3", "4"), "no-rbac-annotation").statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("1"));
        // correct acr -> pass
        stepUpMethodLevelRequest(Set.of("1"), "no-rbac-annotation").statusCode(200).body(is("no-rbac-annotation"));
    }

    @Test
    public void testLocalTokenVerification() {
        // anonymous, no RBAC annotations on endpoint but acr required -> fail
        RestAssured.given()
                .when().get("/tenant-ann-no-oidc-server-step-up-auth")
                .then().statusCode(401);
        Function<Set<String>, ValidatableResponse> request = acrValues -> RestAssured
                .given()
                .auth().oauth2(getAccessTokenVerifiedWithoutOidcServer(acrValues))
                .when().get("/tenant-ann-no-oidc-server-step-up-auth")
                .then();
        // no acr -> fail
        request.apply(Set.of()).statusCode(401);
        // wrong single acr -> fail
        request.apply(Set.of("3")).statusCode(401);
        // wrong multiple acr -> fail
        request.apply(Set.of("3", "4")).statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("1"));
        // correct acr -> pass
        request.apply(Set.of("1")).statusCode(200).body(is("/tenant-ann-no-oidc-server-step-up-auth"));
    }

    @Test
    public void testClassLevelAuthCtxNoRbac() {
        // anonymous, no RBAC annotations on endpoint but acr required -> fail
        RestAssured.given()
                .when().get("/step-up-auth/class-level/no-rbac-annotation")
                .then().statusCode(401);
        // no acr -> fail
        stepUpClassLevelRequest(Set.of(), "no-rbac-annotation").statusCode(401);
        // wrong single acr -> fail
        stepUpClassLevelRequest(Set.of("3"), "no-rbac-annotation").statusCode(401);
        // wrong multiple acr -> fail
        stepUpClassLevelRequest(Set.of("3", "4"), "no-rbac-annotation").statusCode(401);
        // correct acr -> pass
        stepUpClassLevelRequest(Set.of("2"), "no-rbac-annotation").statusCode(200).body(is("no-rbac-annotation"));
    }

    @Test
    public void testMethodLevelAuthCtxRolesAllowed() {
        // no acr -> fail
        stepUpMethodLevelRequest(Set.of(), "user-role").statusCode(401);
        // wrong single acr -> fail
        stepUpMethodLevelRequest(Set.of("1"), "user-role").statusCode(401);
        // wrong multiple acr -> fail
        stepUpMethodLevelRequest(Set.of("1", "4"), "user-role").statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("3"));
        // correct acr & wrong role -> fail
        stepUpMethodLevelRequest(Set.of("3"), "admin-role").statusCode(403);
        // correct acr & correct role -> pass
        stepUpMethodLevelRequest(Set.of("3"), "user-role").statusCode(200).body(is("user-role"));
    }

    @Test
    public void testMethodLevelAuthTenantAnnotationSelection() {
        // wrong acr & correct tenant -> fail
        RestAssured.given()
                .auth().oauth2(getAccessToken(Set.of("3")))
                .when().get("/tenant-ann-step-up-auth/bearer-step-up-auth-1")
                .then()
                .statusCode(401);
        // correct acr & tenant -> pass
        RestAssured.given()
                .auth().oauth2(getAccessToken(Set.of("6")))
                .when().get("/tenant-ann-step-up-auth/bearer-step-up-auth-1")
                .then()
                .statusCode(200)
                .body(is("step-up-auth-annotation-selection"));
        // correct acr & second tenant -> pass
        RestAssured.given()
                .auth().oauth2(getAccessToken(Set.of("6")))
                .when().get("/tenant-ann-step-up-auth/bearer-step-up-auth-2")
                .then()
                .statusCode(200)
                .body(is("step-up-auth-annotation-selection-2"));
    }

    @Test
    public void testClassLevelAuthCtxRolesAllowed() {
        // no acr -> fail
        stepUpClassLevelRequest(Set.of(), "user-role").statusCode(401);
        // wrong single acr -> fail
        stepUpClassLevelRequest(Set.of("1"), "user-role").statusCode(401);
        // wrong multiple acr -> fail
        stepUpClassLevelRequest(Set.of("1", "4"), "user-role").statusCode(401);
        // correct acr & wrong role -> fail
        stepUpClassLevelRequest(Set.of("2"), "admin-role").statusCode(403);
        // correct acr & correct role -> pass
        stepUpClassLevelRequest(Set.of("2"), "user-role").statusCode(200).body(is("user-role"));
    }

    @Test
    public void testMethodLevelMultipleAcrsRequired() {
        // no acr -> fail
        stepUpMethodLevelRequest(Set.of(), "multiple-acr-required").statusCode(401);
        // wrong single acr -> fail
        stepUpMethodLevelRequest(Set.of("4"), "multiple-acr-required").statusCode(401);
        // wrong multiple acr -> fail
        stepUpMethodLevelRequest(Set.of("4", "5"), "multiple-acr-required").statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("1"))
                .header("www-authenticate", containsString("2"))
                .header("www-authenticate", containsString("3"));
        // one wrong, one correct acr -> fail
        stepUpMethodLevelRequest(Set.of("1", "4"), "multiple-acr-required").statusCode(401);
        // one wrong, two correct acrs -> fail
        stepUpMethodLevelRequest(Set.of("1", "2", "4"), "multiple-acr-required").statusCode(401);
        // correct acrs -> pass
        stepUpMethodLevelRequest(Set.of("1", "2", "3"), "multiple-acr-required").statusCode(200)
                .body(is("multiple-acr-required"));
        // correct acrs & an irrelevant extra acr -> pass
        stepUpMethodLevelRequest(Set.of("1", "2", "3", "4"), "multiple-acr-required").statusCode(200)
                .body(is("multiple-acr-required"));
    }

    @Test
    public void testWebSocketsClassLevelAuthContextAnnotation()
            throws ExecutionException, InterruptedException, TimeoutException {
        // wrong acr -> fail
        String wrongToken = getAccessToken(Set.of("3"));
        Assertions.assertThrows(RuntimeException.class, () -> callWebSocketEndpoint(wrongToken, true));
        // correct acr -> pass
        String correctToken = getAccessToken(Set.of("7"));
        callWebSocketEndpoint(correctToken, false);
    }

    @Test
    public void testMaxAgeAndAcrRequired() {
        // no auth_time claim && no acr -> fail
        stepUpMethodLevelRequest(null, "max-age-and-acr-required").statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("max_age"))
                .header("www-authenticate", containsString("120"))
                .header("www-authenticate", containsString("acr_values"))
                .header("www-authenticate", containsString("myACR"));
        // no auth_time claim but iat is correct and acr is correct -> pass
        stepUpMethodLevelRequest(Set.of("myACR"), "max-age-and-acr-required").statusCode(200)
                .body(is("max-age-and-acr-required"));
        // correct acr but (auth_time + max_age < now) -> fail
        stepUpMethodLevelRequest(Set.of("myACR"), "max-age-and-acr-required", 123L).statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("max_age"))
                .header("www-authenticate", containsString("120"))
                .header("www-authenticate", containsString("acr_values"))
                .header("www-authenticate", containsString("myACR"));
        // correct expires at (auth_time + max_age > now) but wrong acr -> fail
        final long nowSecs = System.currentTimeMillis() / 1000;
        stepUpMethodLevelRequest(Set.of("wrongACR"), "max-age-and-acr-required", nowSecs).statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("max_age"))
                .header("www-authenticate", containsString("120"))
                .header("www-authenticate", containsString("acr_values"))
                .header("www-authenticate", containsString("myACR"));
        // correct acr but (auth_time + max_age > now) -> pass
        stepUpMethodLevelRequest(Set.of("myACR"), "max-age-and-acr-required", nowSecs).statusCode(200)
                .body(is("max-age-and-acr-required"));
    }

    private static ValidatableResponse stepUpMethodLevelRequest(Set<String> acrValues, String path) {
        return stepUpMethodLevelRequest(acrValues, path, null);
    }

    private static ValidatableResponse stepUpMethodLevelRequest(Set<String> acrValues, String path,
            Long authTime) {
        return stepUpRequest(acrValues, "method", path, authTime);
    }

    private static ValidatableResponse stepUpClassLevelRequest(Set<String> acrValues, String path) {
        return stepUpRequest(acrValues, "class", path, null);
    }

    private static ValidatableResponse stepUpRequest(Set<String> acrValues, String level, String path,
            Long authTime) {
        return RestAssured.given()
                .auth().oauth2(getAccessTokenVerifiedWithOidcServer(acrValues, authTime))
                .when().get("/step-up-auth/" + level + "-level/" + path)
                .then();
    }

    private static String getAccessToken(Set<String> acrValues) {
        return getAccessTokenVerifiedWithOidcServer(acrValues, null);
    }

    static String getAccessTokenWithAcr(Set<String> acrValues) {
        return getAccessTokenVerifiedWithOidcServer(acrValues, null);
    }

    private static String getAccessTokenVerifiedWithOidcServer(Set<String> acrValues, Long authTime) {
        // get access token from simple OIDC resource
        String json = RestAssured
                .given()
                .queryParam("auth_time", authTime == null ? "" : Long.toString(authTime))
                .queryParam("acr", acrValues == null ? "" : String.join(",", acrValues))
                .when()
                .post("/oidc/accesstoken-with-acr")
                .body().asString();
        JsonObject object = new JsonObject(json);
        return object.getString("access_token");
    }

    private static String getAccessTokenVerifiedWithoutOidcServer(Set<String> acrValues) {
        var jwtBuilder = Jwt.claim("scope", "read:data").preferredUserName("alice").issuer("acceptable-issuer");
        if (acrValues != null) {
            jwtBuilder.claim(Claims.acr, acrValues);
        }
        return jwtBuilder.sign();
    }

    private void callWebSocketEndpoint(String token, boolean expectFailure)
            throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(2);
        List<String> messages = new CopyOnWriteArrayList<>();
        AtomicReference<io.vertx.core.http.WebSocket> ws1 = new AtomicReference<>();
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setHost(websocketAuthCtxUri.getHost());
        options.setPort(websocketAuthCtxUri.getPort());
        options.setURI(websocketAuthCtxUri.getPath());
        if (token != null) {
            options.addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token);
        }
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        try {
            client
                    .connect(options)
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            io.vertx.core.http.WebSocket ws = r.result();
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
                Assertions.assertEquals("step-up-auth-annotation-selection echo: hello", messages.get(1));
            }
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Tenant("tenant-public-key")
    @Path("/tenant-ann-no-oidc-server-step-up-auth")
    public static class TenantAnnotationNoOidcServerStepUpAuthResource {
        @GET
        @AuthenticationContext("1")
        public String noRbacAnnotationMethodLevel() {
            return "/tenant-ann-no-oidc-server-step-up-auth";
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
        @RolesAllowed("user")
        @Path("user-role")
        public String userRoleMethodLevel() {
            return "user-role";
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

        @GET
        @AuthenticationContext(maxAge = "PT120s", value = "myACR")
        @Path("max-age-and-acr-required")
        public String maxAgeAndAcrRequired() {
            return "max-age-and-acr-required";
        }
    }

    @Path("/tenant-ann-step-up-auth")
    public static class TenantAnnotationStepUpAuthResource {

        @Inject
        RoutingContext routingContext;

        @Tenant("step-up-auth-annotation-selection")
        @AuthenticationContext("6")
        @GET
        @Path("/bearer-step-up-auth-1")
        public String firstTenantSelectedMethodLevel() {
            return getTenantId();
        }

        @Tenant("step-up-auth-annotation-selection-2")
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
        @RolesAllowed("user")
        @Path("user-role")
        public String userRoleMethodLevel() {
            return "user-role";
        }

        @GET
        @RolesAllowed("admin")
        @Path("admin-role")
        public String adminRoleMethodLevel() {
            return "admin-role";
        }
    }

    @AuthenticationContext("7")
    @Tenant("step-up-auth-annotation-selection")
    @WebSocket(path = "/ws/tenant-annotation/bearer-step-up-auth")
    public static class WebSocketEndpointWithTenantAnnotationAndAuthCtx {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return "step-up-auth-annotation-selection echo: " + message;
        }

    }

    public static class StepUpAuthTestProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "step-up-auth";
        }
    }
}
