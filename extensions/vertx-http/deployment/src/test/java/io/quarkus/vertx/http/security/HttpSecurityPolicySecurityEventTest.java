package io.quarkus.vertx.http.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.PathMatchingHttpSecurityPolicy;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class HttpSecurityPolicySecurityEventTest {

    private static final String APP_PROPS = """
            quarkus.http.auth.permission.authenticated.paths=/authenticated
            quarkus.http.auth.permission.authenticated.policy=authenticated
            quarkus.http.auth.permission.deny-all.paths=/deny
            quarkus.http.auth.permission.deny-all.policy=deny
            quarkus.http.auth.permission.permit-all.paths=/permit
            quarkus.http.auth.permission.permit-all.policy=permit
            quarkus.http.auth.permission.roles.paths=/roles
            quarkus.http.auth.permission.roles.policy=roles
            quarkus.http.auth.policy.roles.roles-allowed=admin
            quarkus.http.auth.permission.custom-named.paths=/custom-named
            quarkus.http.auth.permission.custom-named.policy=custom-named
            quarkus.http.auth.permission.map-roles.paths=/map-roles
            quarkus.http.auth.permission.map-roles.policy=map-roles
            quarkus.http.auth.policy.map-roles.roles-allowed=admin
            quarkus.http.auth.policy.map-roles.roles.test=admin
            """;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class, EventObserver.class,
                    CustomNamedHttpSecurityPolicy.class, GlobalCustomHttpSecurityPolicy.class)
            .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @Inject
    EventObserver observer;

    @BeforeEach
    public void clear() {
        observer.authZFailureStorage.clear();
        observer.asyncAllEventsStorage.clear();
        observer.asyncAuthNFailureEventStorage.clear();
        observer.allEventsStorage.clear();
        observer.authZSuccessStorage.clear();
        observer.authNSuccessStorage.clear();
    }

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("test", "test", "test")
                .add("admin", "admin", "admin");
    }

    @Test
    public void testAuthenticationEvents() {
        RestAssured.given().auth().preemptive().basic("unknown", "unknown").get("/authenticated").then().statusCode(401);
        assertEquals(0, observer.authZFailureStorage.size());
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, observer.asyncAuthNFailureEventStorage.size()));
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, observer.asyncAllEventsStorage.size()));
        assertEquals(1, observer.allEventsStorage.size());
        AuthenticationFailureEvent event = observer.asyncAuthNFailureEventStorage.get(0);
        assertNull(event.getSecurityIdentity());
        assertNotNull(event.getEventProperties().get(RoutingContext.class.getName()));
        assertTrue(event.getAuthenticationFailure() instanceof AuthenticationFailedException);
    }

    @Test
    public void testAuthenticatedPolicy() {
        RestAssured.given().auth().preemptive().basic("test", "test").get("/authenticated").then().statusCode(200);
        assertEquals(0, observer.authZFailureStorage.size());
        assertEquals(1, observer.authZSuccessStorage.size());
        assertEquals(1, observer.authNSuccessStorage.size());
        AuthenticationSuccessEvent successEvent = observer.authNSuccessStorage.get(0);
        assertNotNull(successEvent.getSecurityIdentity());
        assertEquals("test", successEvent.getSecurityIdentity().getPrincipal().getName());
        RoutingContext routingContext = (RoutingContext) successEvent.getEventProperties().get(RoutingContext.class.getName());
        assertNotNull(routingContext);
        assertTrue(routingContext.request().path().endsWith("/authenticated"));
        assertEquals(1, observer.authZSuccessStorage.size());
        AuthorizationSuccessEvent authorizationSuccessEvent = observer.authZSuccessStorage.get(0);
        assertEquals(successEvent.getSecurityIdentity(), authorizationSuccessEvent.getSecurityIdentity());
        assertEquals(routingContext, authorizationSuccessEvent.getEventProperties().get(RoutingContext.class.getName()));
        RestAssured.given().get("/authenticated").then().statusCode(401);
        assertEquals(1, observer.authZFailureStorage.size());
        AuthorizationFailureEvent event = observer.authZFailureStorage.get(0);
        SecurityIdentity identity = event.getSecurityIdentity();
        assertNotNull(identity);
        assertNotNull(event.getEventProperties().get(RoutingContext.class.getName()));
        assertEquals(PathMatchingHttpSecurityPolicy.class.getName(), event.getAuthorizationContext());
        assertTrue(identity.isAnonymous());
        assertEquals(3, observer.allEventsStorage.size());
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(3, observer.asyncAllEventsStorage.size()));
        AuthenticationSuccessEvent authNSuccessEvent = (AuthenticationSuccessEvent) observer.allEventsStorage.get(0);
        identity = authNSuccessEvent.getSecurityIdentity();
        assertNotNull(identity);
        assertEquals("test", identity.getPrincipal().getName());
        assertNotNull(authNSuccessEvent.getEventProperties().get(RoutingContext.class.getName()));
    }

    @Test
    public void testPermitAllPolicy() {
        RestAssured.get("/permit").then().statusCode(200);
        assertEquals(0, observer.authZFailureStorage.size());
        assertEquals(0, observer.authNSuccessStorage.size());
        assertEquals(1, observer.allEventsStorage.size());
        assertEquals(1, observer.authZSuccessStorage.size());
        AuthorizationSuccessEvent event = observer.authZSuccessStorage.get(0);
        assertNotNull(event.getSecurityIdentity());
        assertTrue(event.getSecurityIdentity().isAnonymous());
        assertNotNull(event.getEventProperties().get(RoutingContext.class.getName()));
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, observer.asyncAllEventsStorage.size()));
    }

    @Test
    public void testRolesPolicy() {
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/roles").then().statusCode(200);
        assertEquals(0, observer.authZFailureStorage.size());
        assertEquals(2, observer.allEventsStorage.size());
        assertEquals(1, observer.authNSuccessStorage.size());
        AuthenticationSuccessEvent successEvent = observer.authNSuccessStorage.get(0);
        SecurityIdentity identity = successEvent.getSecurityIdentity();
        assertNotNull(identity);
        assertEquals("admin", identity.getPrincipal().getName());
        RoutingContext routingContext = (RoutingContext) successEvent.getEventProperties().get(RoutingContext.class.getName());
        assertNotNull(routingContext);
        assertTrue(routingContext.request().path().endsWith("/roles"));
        assertEquals(1, observer.authZSuccessStorage.size());
        AuthorizationSuccessEvent authorizationSuccessEvent = observer.authZSuccessStorage.get(0);
        assertEquals(identity, authorizationSuccessEvent.getSecurityIdentity());
        assertEquals(routingContext, authorizationSuccessEvent.getEventProperties().get(RoutingContext.class.getName()));
        RestAssured.given().auth().preemptive().basic("test", "test").get("/roles").then().statusCode(403);
        assertEquals(1, observer.authZFailureStorage.size());
        AuthorizationFailureEvent event = observer.authZFailureStorage.get(0);
        assertEquals(PathMatchingHttpSecurityPolicy.class.getName(), event.getAuthorizationContext());
        identity = event.getSecurityIdentity();
        assertNotNull(identity);
        assertEquals("test", identity.getPrincipal().getName());
        assertTrue(event.getAuthorizationFailure() instanceof ForbiddenException);
        assertNotNull(event.getEventProperties().get(RoutingContext.class.getName()));
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(4, observer.asyncAllEventsStorage.size()));
    }

    @Test
    public void testRolesPolicyAugmentation() {
        RestAssured.given().auth().preemptive().basic("test", "test").get("/map-roles").then().statusCode(200);
        assertEquals(0, observer.authZFailureStorage.size());
        assertEquals(2, observer.allEventsStorage.size());
        assertEquals(1, observer.authNSuccessStorage.size());
        assertEquals(1, observer.authZSuccessStorage.size());
        SecurityIdentity originalIdentity = observer.authNSuccessStorage.get(0).getSecurityIdentity();
        SecurityIdentity augmentedIdentity = observer.authZSuccessStorage.get(0).getSecurityIdentity();
        assertNotEquals(originalIdentity, augmentedIdentity);
        assertTrue(augmentedIdentity.hasRole("admin"));
        assertFalse(originalIdentity.hasRole("admin"));
    }

    @Test
    public void testDenyAllPolicy() {
        RestAssured.get("/deny").then().statusCode(401);
        assertEquals(1, observer.authZFailureStorage.size());
        assertEquals(0, observer.authZSuccessStorage.size());
        assertEquals(0, observer.authNSuccessStorage.size());
        AuthorizationFailureEvent first = observer.authZFailureStorage.get(0);
        SecurityIdentity identity = first.getSecurityIdentity();
        assertNotNull(identity);
        assertTrue(identity.isAnonymous());
        assertNull(first.getAuthorizationFailure());
        assertEquals(PathMatchingHttpSecurityPolicy.class.getName(), first.getAuthorizationContext());
        assertNotNull(first.getEventProperties().get(RoutingContext.class.getName()));
        RestAssured.given().auth().preemptive().basic("test", "test").get("/deny").then().statusCode(403);
        assertEquals(2, observer.authZFailureStorage.size());
        AuthorizationFailureEvent second = observer.authZFailureStorage.get(1);
        identity = second.getSecurityIdentity();
        assertNotNull(identity);
        assertFalse(identity.isAnonymous());
        assertEquals("test", identity.getPrincipal().getName());
        assertNull(first.getAuthorizationFailure());
        assertEquals(PathMatchingHttpSecurityPolicy.class.getName(), first.getAuthorizationContext());
        assertNotNull(first.getEventProperties().get(RoutingContext.class.getName()));
        assertTrue(second.getAuthorizationFailure() instanceof ForbiddenException);
        assertEquals(PathMatchingHttpSecurityPolicy.class.getName(), first.getAuthorizationContext());
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(3, observer.asyncAllEventsStorage.size()));
        assertEquals(3, observer.allEventsStorage.size());
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertEquals(1,
                observer.asyncAllEventsStorage.stream().filter(se -> se instanceof AuthenticationSuccessEvent).count()));
        AuthenticationSuccessEvent event = (AuthenticationSuccessEvent) observer.asyncAllEventsStorage.stream()
                .filter(se -> se instanceof AuthenticationSuccessEvent).findFirst().get();
        assertNotNull(event.getSecurityIdentity());
        assertFalse(event.getSecurityIdentity().isAnonymous());
        RoutingContext routingContext = (RoutingContext) event.getEventProperties().get(RoutingContext.class.getName());
        assertNotNull(routingContext);
        assertTrue(routingContext.request().path().endsWith("/deny"));
    }

    @Test
    public void testNamedCustomPolicy() {
        RestAssured.given().get("/custom-named").then().statusCode(200);
        assertTrue(observer.authZFailureStorage.isEmpty());
        assertEquals(1, observer.authZSuccessStorage.size());
        AuthorizationSuccessEvent successEvent = observer.authZSuccessStorage.get(0);
        SecurityIdentity identity = successEvent.getSecurityIdentity();
        assertTrue(identity.isAnonymous());
        assertNotNull(successEvent.getEventProperties().get(RoutingContext.class.getName()));
        RestAssured.given().header("custom-named", "ignored").get("/custom-named").then().statusCode(401);
        assertEquals(1, observer.authZFailureStorage.size());
        AuthorizationFailureEvent event = observer.authZFailureStorage.get(0);
        identity = event.getSecurityIdentity();
        assertNotNull(identity);
        assertTrue(identity.isAnonymous());
        assertNotNull(event.getEventProperties().get(RoutingContext.class.getName()));
        assertEquals(2, observer.allEventsStorage.size());
        assertEquals(event, observer.allEventsStorage.get(1));
        assertEquals(PathMatchingHttpSecurityPolicy.class.getName(), event.getAuthorizationContext());
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(2, observer.asyncAllEventsStorage.size()));
    }

    @Test
    public void testGlobalCustomPolicy() {
        RestAssured.given().get("/custom-global").then().statusCode(200);
        assertTrue(observer.authZFailureStorage.isEmpty());
        assertEquals(1, observer.allEventsStorage.size());
        assertEquals(1, observer.authZSuccessStorage.size());
        AuthorizationSuccessEvent successEvent = observer.authZSuccessStorage.get(0);
        assertNotNull(successEvent.getSecurityIdentity());
        assertTrue(successEvent.getSecurityIdentity().isAnonymous());
        RoutingContext routingContext = (RoutingContext) successEvent.getEventProperties().get(RoutingContext.class.getName());
        assertNotNull(routingContext);
        assertTrue(routingContext.request().path().endsWith("/custom-global"));
        RestAssured.given().header("custom-global", "ignored").get("/custom-global").then().statusCode(401);
        assertEquals(1, observer.authZFailureStorage.size());
        AuthorizationFailureEvent event = observer.authZFailureStorage.get(0);
        SecurityIdentity identity = event.getSecurityIdentity();
        assertNotNull(identity);
        assertTrue(identity.isAnonymous());
        assertNotNull(event.getEventProperties().get(RoutingContext.class.getName()));
        assertTrue(event.getAuthorizationContext().contains("GlobalCustomHttpSecurityPolicy"));
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(2, observer.asyncAllEventsStorage.size()));
        assertEquals(2, observer.allEventsStorage.size());
    }

    @Singleton
    public static class EventObserver {
        private final List<AuthorizationFailureEvent> authZFailureStorage = new CopyOnWriteArrayList<>();
        private final List<AuthenticationFailureEvent> asyncAuthNFailureEventStorage = new CopyOnWriteArrayList<>();
        private final List<SecurityEvent> asyncAllEventsStorage = new CopyOnWriteArrayList<>();
        private final List<SecurityEvent> allEventsStorage = new CopyOnWriteArrayList<>();
        private final List<AuthenticationSuccessEvent> authNSuccessStorage = new CopyOnWriteArrayList<>();
        private final List<AuthorizationSuccessEvent> authZSuccessStorage = new CopyOnWriteArrayList<>();

        void observeEvents(@ObservesAsync AuthenticationFailureEvent event) {
            asyncAuthNFailureEventStorage.add(event);
        }

        void observeEvents(@Observes AuthorizationFailureEvent event) {
            authZFailureStorage.add(event);
        }

        void observeAllEventsAsync(@ObservesAsync SecurityEvent event) {
            asyncAllEventsStorage.add(event);
        }

        void observeAllEventsSync(@Observes SecurityEvent event) {
            allEventsStorage.add(event);
        }

        void observeAuthNSuccess(@Observes AuthenticationSuccessEvent event) {
            authNSuccessStorage.add(event);
        }

        void observeAuthZSuccess(@Observes AuthorizationSuccessEvent event) {
            authZSuccessStorage.add(event);
        }
    }

    @ApplicationScoped
    public static class CustomNamedHttpSecurityPolicy implements HttpSecurityPolicy {

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext ctx, Uni<SecurityIdentity> identity,
                AuthorizationRequestContext requestContext) {
            if (ctx.request().path().endsWith("/custom-named") && ctx.request().headers().contains("custom-named")) {
                return Uni.createFrom().item(CheckResult.DENY);
            }
            return Uni.createFrom().item(CheckResult.PERMIT);
        }

        @Override
        public String name() {
            return "custom-named";
        }
    }

    @ApplicationScoped
    public static class GlobalCustomHttpSecurityPolicy implements HttpSecurityPolicy {

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext ctx, Uni<SecurityIdentity> identity,
                AuthorizationRequestContext requestContext) {
            if (ctx.request().path().endsWith("/custom-global") && ctx.request().headers().contains("custom-global")) {
                return Uni.createFrom().item(CheckResult.DENY);
            }
            return Uni.createFrom().item(CheckResult.PERMIT);
        }
    }

}
