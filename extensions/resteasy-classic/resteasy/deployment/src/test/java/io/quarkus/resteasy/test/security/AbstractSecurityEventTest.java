package io.quarkus.resteasy.test.security;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.restassured.RestAssured;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractSecurityEventTest {

    protected static final Class<?>[] TEST_CLASSES = {
            RolesAllowedResource.class, TestIdentityProvider.class, TestIdentityController.class,
            UnsecuredResource.class, UnsecuredSubResource.class, EventObserver.class, UnsecuredResourceInterface.class,
            UnsecuredParentResource.class, RolesAllowedService.class, RolesAllowedServiceResource.class
    };

    @Inject
    EventObserver observer;

    @Inject
    HttpBuildTimeConfig httpBuildTimeConfig;

    @BeforeEach
    public void clean() {
        observer.asyncAuthZFailureEvents.clear();
        observer.syncEvents.clear();
        observer.authNFailureEvents.clear();
    }

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    private boolean isProactiveAuth() {
        return httpBuildTimeConfig.auth.proactive;
    }

    @Test
    public void testAuthenticationEvent() {
        RestAssured.given().auth().preemptive().basic("unknown", "unknown").get("/unsecured/authenticated").then()
                .statusCode(401);
        assertEquals(1, observer.authNFailureEvents.size());
        AuthenticationFailureEvent event = observer.authNFailureEvents.get(0);
        assertNull(event.getSecurityIdentity());
        assertTrue(event.getAuthenticationFailure() instanceof AuthenticationFailedException);
        assertNotNull(event.getEventProperties().get(RoutingContext.class.getName()));
    }

    @Test
    public void testRolesAllowed() {
        RestAssured.get("/roles").then().statusCode(401);
        assertSyncObserved(1);
        assertAsyncAuthZFailureObserved(1);
        SecurityIdentity anonymousIdentity = observer.syncEvents.get(0).getSecurityIdentity();
        assertNotNull(anonymousIdentity);
        assertTrue(anonymousIdentity.isAnonymous());
        RestAssured.given().auth().preemptive().basic("user", "user").get("/roles/admin").then().statusCode(403);
        assertSyncObserved(3);
        AuthenticationSuccessEvent successEvent = (AuthenticationSuccessEvent) observer.syncEvents.get(1);
        SecurityIdentity identity = successEvent.getSecurityIdentity();
        assertNotNull(identity);
        assertEquals("user", identity.getPrincipal().getName());
        RoutingContext routingContext = (RoutingContext) successEvent.getEventProperties().get(RoutingContext.class.getName());
        assertTrue(routingContext.request().path().endsWith("/roles/admin"));
        assertAsyncAuthZFailureObserved(2);
    }

    @Test
    public void testNestedRolesAllowed() {
        // there are 2 different checks in place: user & admin on resource, admin on service
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/roles-service/hello").then().statusCode(200)
                .body(is(RolesAllowedService.SERVICE_HELLO));
        assertSyncObserved(3);
        AuthenticationSuccessEvent successEvent = (AuthenticationSuccessEvent) observer.syncEvents.get(0);
        SecurityIdentity identity = successEvent.getSecurityIdentity();
        assertNotNull(identity);
        assertEquals("admin", identity.getPrincipal().getName());
        RoutingContext routingContext = (RoutingContext) successEvent.getEventProperties().get(RoutingContext.class.getName());
        assertNotNull(routingContext);
        assertTrue(routingContext.request().path().endsWith("/roles-service/hello"));
        // authorization success on endpoint
        AuthorizationSuccessEvent authZSuccessEvent = (AuthorizationSuccessEvent) observer.syncEvents.get(1);
        assertEquals(identity, authZSuccessEvent.getSecurityIdentity());
        identity = authZSuccessEvent.getSecurityIdentity();
        assertEquals(routingContext, authZSuccessEvent.getEventProperties().get(RoutingContext.class.getName()));
        String securedMethod = (String) authZSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.SECURED_METHOD_KEY);
        assertEquals("io.quarkus.resteasy.test.security.RolesAllowedServiceResource#getServiceHello",
                securedMethod);
        // authorization success on service level performed by CDI interceptor
        authZSuccessEvent = (AuthorizationSuccessEvent) observer.syncEvents.get(2);
        securedMethod = (String) authZSuccessEvent.getEventProperties().get(AuthorizationSuccessEvent.SECURED_METHOD_KEY);
        assertEquals("io.quarkus.resteasy.test.security.RolesAllowedService#hello", securedMethod);
        assertEquals(identity, authZSuccessEvent.getSecurityIdentity());
        assertNotNull(authZSuccessEvent.getEventProperties().get(RoutingContext.class.getName()));
        assertAsyncAuthZFailureObserved(0);
        RestAssured.given().auth().preemptive().basic("user", "user").get("/roles-service/hello").then().statusCode(403);
        assertSyncObserved(6);
        // "roles-service" Jakarta REST resource requires 'admin' or 'user' role, therefore check succeeds
        successEvent = (AuthenticationSuccessEvent) observer.syncEvents.get(3);
        identity = successEvent.getSecurityIdentity();
        assertNotNull(identity);
        assertEquals("user", identity.getPrincipal().getName());
        routingContext = (RoutingContext) successEvent.getEventProperties().get(RoutingContext.class.getName());
        assertNotNull(routingContext);
        assertTrue(routingContext.request().path().endsWith("/roles-service/hello"));
        authZSuccessEvent = (AuthorizationSuccessEvent) observer.syncEvents.get(4);
        assertEquals(identity, authZSuccessEvent.getSecurityIdentity());
        assertEquals(routingContext, authZSuccessEvent.getEventProperties().get(RoutingContext.class.getName()));
        // RolesService requires 'admin' role, therefore user fails
        assertAsyncAuthZFailureObserved(1);
        AuthorizationFailureEvent authZFailureEvent = observer.asyncAuthZFailureEvents.get(0);
        securedMethod = (String) authZFailureEvent.getEventProperties().get(AuthorizationFailureEvent.SECURED_METHOD_KEY);
        assertEquals("io.quarkus.resteasy.test.security.RolesAllowedService#hello", securedMethod);
        SecurityIdentity userIdentity = authZFailureEvent.getSecurityIdentity();
        assertNotNull(userIdentity);
        assertTrue(userIdentity.hasRole("user"));
        assertEquals("user", userIdentity.getPrincipal().getName());
        assertNotNull(authZFailureEvent.getEventProperties().get(RoutingContext.class.getName()));
        assertEquals(RolesAllowedCheck.class.getName(), authZFailureEvent.getAuthorizationContext());
    }

    @Test
    public void testNestedPermitAll() {
        // @PermitAll is on CDI bean but resource is not secured
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/roles-service/bye").then().statusCode(200)
                .body(is(RolesAllowedService.SERVICE_BYE));
        final int expectedEventsCount;
        if (isProactiveAuth()) {
            // auth + @PermitAll
            expectedEventsCount = 2;
        } else {
            // @PermitAll
            expectedEventsCount = 1;
        }
        assertSyncObserved(expectedEventsCount, true);

        if (expectedEventsCount == 2) {
            AuthenticationSuccessEvent successEvent = (AuthenticationSuccessEvent) observer.syncEvents.get(0);
            SecurityIdentity identity = successEvent.getSecurityIdentity();
            assertNotNull(identity);
            assertEquals("admin", identity.getPrincipal().getName());
            RoutingContext routingContext = (RoutingContext) successEvent.getEventProperties()
                    .get(RoutingContext.class.getName());
            assertNotNull(routingContext);
            assertTrue(routingContext.request().path().endsWith("/roles-service/bye"));
        }
        // authorization success on service level performed by CDI interceptor
        var authZSuccessEvent = (AuthorizationSuccessEvent) observer.syncEvents.get(expectedEventsCount - 1);
        String securedMethod = (String) authZSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.SECURED_METHOD_KEY);
        assertEquals("io.quarkus.resteasy.test.security.RolesAllowedService#bye", securedMethod);
        assertNotNull(authZSuccessEvent.getEventProperties().get(RoutingContext.class.getName()));
        if (isProactiveAuth()) {
            assertNotNull(authZSuccessEvent.getSecurityIdentity());
            assertEquals("admin", authZSuccessEvent.getSecurityIdentity().getPrincipal().getName());
        }
        assertAsyncAuthZFailureObserved(0);
    }

    @Test
    public void testAuthenticated() {
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/unsecured/authenticated").then().statusCode(200)
                .body(is("authenticated"));
        assertSyncObserved(2);
        assertAsyncAuthZFailureObserved(0);
        AuthenticationSuccessEvent authNSuccess = (AuthenticationSuccessEvent) observer.syncEvents.get(0);
        SecurityIdentity identity = authNSuccess.getSecurityIdentity();
        assertNotNull(identity);
        assertEquals("admin", identity.getPrincipal().getName());
        AuthorizationSuccessEvent authZSuccess = (AuthorizationSuccessEvent) observer.syncEvents.get(1);
        assertEquals(identity, authZSuccess.getSecurityIdentity());
        RestAssured.given().get("/unsecured/authenticated").then().statusCode(401);
        assertSyncObserved(3);
        assertAsyncAuthZFailureObserved(1);
        assertEquals(1, observer.asyncAuthZFailureEvents.size());
        AuthorizationFailureEvent authZFailure = observer.asyncAuthZFailureEvents.get(0);
        SecurityIdentity anonymousIdentity = authZFailure.getSecurityIdentity();
        assertNotNull(anonymousIdentity);
        assertTrue(anonymousIdentity.isAnonymous());
        String securedMethod = (String) authZFailure.getEventProperties().get(AuthorizationFailureEvent.SECURED_METHOD_KEY);
        assertEquals("io.quarkus.resteasy.test.security.UnsecuredResource#authenticated", securedMethod);
    }

    @Test
    public void testDenyAll() {
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/unsecured/denyAll").then().statusCode(403);
        assertSyncObserved(2);
        assertAsyncAuthZFailureObserved(1);
        AuthorizationFailureEvent authZFailure = (AuthorizationFailureEvent) observer.syncEvents.get(1);
        SecurityIdentity adminIdentity = authZFailure.getSecurityIdentity();
        assertNotNull(adminIdentity);
        assertEquals("admin", adminIdentity.getPrincipal().getName());
        assertTrue(adminIdentity.hasRole("admin"));
        assertEquals(adminIdentity, observer.asyncAuthZFailureEvents.get(0).getSecurityIdentity());
        AuthenticationSuccessEvent authNSuccess = (AuthenticationSuccessEvent) observer.syncEvents.get(0);
        assertEquals(adminIdentity, authNSuccess.getSecurityIdentity());
        RestAssured.given().get("/unsecured/authenticated").then().statusCode(401);
        assertSyncObserved(3);
        assertAsyncAuthZFailureObserved(2);
    }

    @Test
    public void testPermitAll() {
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/unsecured/permitAll").then().statusCode(200)
                .body(is("permitAll"));
        assertAsyncAuthZFailureObserved(0);
        // permit all check does not require authentication, hence auth success is not fired
        if (isProactiveAuth()) {
            assertSyncObserved(2, true);
            AuthenticationSuccessEvent successEvent = (AuthenticationSuccessEvent) observer.syncEvents.get(0);
            SecurityIdentity identity = successEvent.getSecurityIdentity();
            assertNotNull(identity);
            assertEquals("admin", identity.getPrincipal().getName());
            RoutingContext routingContext = (RoutingContext) successEvent.getEventProperties()
                    .get(RoutingContext.class.getName());
            assertNotNull(routingContext);
            assertTrue(routingContext.request().path().endsWith("/unsecured/permitAll"));
            AuthorizationSuccessEvent authZSuccessEvent = (AuthorizationSuccessEvent) observer.syncEvents.get(1);
            assertNotNull(authZSuccessEvent.getSecurityIdentity());
        } else {
            assertSyncObserved(1, true);
            AuthorizationSuccessEvent authZSuccessEvent = (AuthorizationSuccessEvent) observer.syncEvents.get(0);
            assertNull(authZSuccessEvent.getSecurityIdentity());
        }
        RestAssured.given().get("/unsecured/permitAll").then().statusCode(200).body(is("permitAll"));
        if (isProactiveAuth()) {
            assertSyncObserved(3, true);
        } else {
            assertSyncObserved(2, true);
        }
        assertAsyncAuthZFailureObserved(0);
    }

    private void assertSyncObserved(int count) {
        assertSyncObserved(count, false);
    }

    private void assertSyncObserved(int count, boolean isPermitAll) {
        assertEquals(count, observer.syncEvents.size());
        if (count > 0) {
            if (!isPermitAll) {
                assertTrue(observer.syncEvents.stream().allMatch(e -> e.getSecurityIdentity() != null));
            }
        }
    }

    private void assertAsyncAuthZFailureObserved(int count) {
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(count, observer.asyncAuthZFailureEvents.size()));
        if (count > 0) {
            assertTrue(observer.asyncAuthZFailureEvents.stream().allMatch(e -> e.getSecurityIdentity() != null));
            assertTrue(observer.asyncAuthZFailureEvents.stream()
                    .map(e -> e.getEventProperties().get(RoutingContext.class.getName()))
                    .allMatch(Objects::nonNull));
        }
    }

    @Singleton
    public static class EventObserver {

        private final List<SecurityEvent> syncEvents = new CopyOnWriteArrayList<>();
        private final List<AuthorizationFailureEvent> asyncAuthZFailureEvents = new CopyOnWriteArrayList<>();
        private final List<AuthenticationFailureEvent> authNFailureEvents = new CopyOnWriteArrayList<>();

        void observe(@Observes SecurityEvent securityEvent) {
            syncEvents.add(securityEvent);
        }

        void observe(@Observes AuthenticationFailureEvent authenticationFailureEvent) {
            authNFailureEvents.add(authenticationFailureEvent);
        }

        void observe(@ObservesAsync AuthorizationFailureEvent authorizationFailureEvent) {
            asyncAuthZFailureEvents.add(authorizationFailureEvent);
        }
    }
}
