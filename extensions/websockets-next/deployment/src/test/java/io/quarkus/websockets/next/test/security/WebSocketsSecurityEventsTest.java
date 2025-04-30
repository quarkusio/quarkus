package io.quarkus.websockets.next.test.security;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.runtime.SecurityHttpUpgradeCheck;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;

public class WebSocketsSecurityEventsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.permission.roles.paths=/http-upgrade-config-endpoint*
                            quarkus.http.auth.permission.roles.policy=roles
                            quarkus.http.auth.policy.roles.roles-allowed=http-upgrade-config
                            """), "application.properties")
                    .addClasses(WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            HttpUpgradeAnnotationEndpoint.class, HttpUpgradeConfigEndpoint.class, OnTextMessageEndpoint.class,
                            SecurityEventObserver.class));

    @TestHTTPResource("http-upgrade-annotation-endpoint")
    URI httpUpgradeAnnotationEndpoint;

    @TestHTTPResource("http-upgrade-config-endpoint")
    URI httpUpgradeConfigEndpoint;

    @TestHTTPResource("on-text-message-endpoint")
    URI onTextMessageAnnotationEndpoint;

    @Inject
    Vertx vertx;

    @Inject
    SecurityEventObserver eventObserver;

    @BeforeEach
    public void clearEvents() {
        eventObserver.clearEvents();
    }

    @Test
    public void testHttpUpgradeSecuredWithAnnotationEvents() {
        // user is missing role 'http-upgrade-annotation' -> fail
        TestIdentityController.resetRoles().add("user", "user");
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(basicAuth(), httpUpgradeAnnotationEndpoint));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("403"));
        }

        assertEquals(1, eventObserver.authenticationSuccessEvents);
        assertEquals(1, eventObserver.authenticationSuccessAsyncEvents);
        assertEquals(1, eventObserver.authorizationFailureEvents);
        assertEquals(1, eventObserver.authorizationFailureAsyncEvents);
        assertEquals(0, eventObserver.authorizationSuccessEvents);
        assertEquals(0, eventObserver.authorizationSuccessAsyncEvents);
        assertEquals(0, eventObserver.authenticationFailureEvents);
        assertEquals(0, eventObserver.authenticationFailureAsyncEvents);
        assertAuthenticationSuccess();
        AuthorizationFailureEvent authorizationFailureEvent = assertAuthorizationFailureEvent(
                eventObserver.authorizationFailureEvents);
        HttpServerRequest httpServerRequest = (HttpServerRequest) authorizationFailureEvent.getEventProperties()
                .get(SecurityHttpUpgradeCheck.HTTP_REQUEST_KEY);
        Assertions.assertNotNull(httpServerRequest);
        authorizationFailureEvent = assertAuthorizationFailureEvent(eventObserver.authorizationFailureAsyncEvents);
        httpServerRequest = (HttpServerRequest) authorizationFailureEvent.getEventProperties()
                .get(SecurityHttpUpgradeCheck.HTTP_REQUEST_KEY);
        Assertions.assertNotNull(httpServerRequest);
        String endpointId = (String) authorizationFailureEvent.getEventProperties()
                .get(SecurityHttpUpgradeCheck.SECURED_ENDPOINT_ID_KEY);
        Assertions.assertEquals("one-two-three", endpointId);
        eventObserver.clearEvents();

        // user has role 'http-upgrade-annotation' -> succeed
        TestIdentityController.resetRoles().add("user", "user", "http-upgrade-annotation");
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth(), httpUpgradeAnnotationEndpoint);
            client.waitForMessages(1);
            Assertions.assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            Assertions.assertEquals("hello", client.getMessages().get(1).toString());
        }
        assertEquals(1, eventObserver.authenticationSuccessEvents);
        assertEquals(1, eventObserver.authenticationSuccessAsyncEvents);
        assertEquals(0, eventObserver.authorizationFailureEvents);
        assertEquals(0, eventObserver.authorizationFailureAsyncEvents);
        assertEquals(1, eventObserver.authorizationSuccessEvents);
        assertEquals(1, eventObserver.authorizationSuccessAsyncEvents);
        assertEquals(0, eventObserver.authenticationFailureEvents);
        assertEquals(0, eventObserver.authenticationFailureAsyncEvents);
        assertAuthenticationSuccess();
        AuthorizationSuccessEvent authorizationSuccessEvent = assertAuthorizationSuccessEvent(
                eventObserver.authorizationSuccessEvents);
        String actualAuthZCtx = (String) authorizationSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT);
        Assertions.assertNotNull(actualAuthZCtx);
        Assertions.assertTrue(actualAuthZCtx.contains("RolesAllowed"));
        httpServerRequest = (HttpServerRequest) authorizationSuccessEvent.getEventProperties()
                .get(SecurityHttpUpgradeCheck.HTTP_REQUEST_KEY);
        Assertions.assertNotNull(httpServerRequest);
        authorizationSuccessEvent = assertAuthorizationSuccessEvent(eventObserver.authorizationSuccessAsyncEvents);
        actualAuthZCtx = (String) authorizationSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT);
        Assertions.assertNotNull(actualAuthZCtx);
        Assertions.assertTrue(actualAuthZCtx.contains("RolesAllowed"));
        httpServerRequest = (HttpServerRequest) authorizationSuccessEvent.getEventProperties()
                .get(SecurityHttpUpgradeCheck.HTTP_REQUEST_KEY);
        Assertions.assertNotNull(httpServerRequest);
        endpointId = (String) authorizationSuccessEvent.getEventProperties()
                .get(SecurityHttpUpgradeCheck.SECURED_ENDPOINT_ID_KEY);
        Assertions.assertEquals("one-two-three", endpointId);
    }

    @Test
    public void testHttpUpgradeSecuredWithConfigurationEvents() {
        // user is missing role 'http-upgrade-config' -> fail
        TestIdentityController.resetRoles().add("user", "user");
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(basicAuth(), httpUpgradeConfigEndpoint));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("403"));
        }
        assertEquals(1, eventObserver.authenticationSuccessEvents);
        assertEquals(1, eventObserver.authenticationSuccessAsyncEvents);
        assertEquals(1, eventObserver.authorizationFailureEvents);
        assertEquals(1, eventObserver.authorizationFailureAsyncEvents);
        assertEquals(0, eventObserver.authorizationSuccessEvents);
        assertEquals(0, eventObserver.authorizationSuccessAsyncEvents);
        assertEquals(0, eventObserver.authenticationFailureEvents);
        assertEquals(0, eventObserver.authenticationFailureAsyncEvents);
        assertAuthenticationSuccess();
        AuthorizationFailureEvent authorizationFailureEvent = assertAuthorizationFailureEvent(
                eventObserver.authorizationFailureEvents);
        RoutingContext routingContext = (RoutingContext) authorizationFailureEvent.getEventProperties()
                .get(RoutingContext.class.getName());
        Assertions.assertNotNull(routingContext);
        authorizationFailureEvent = assertAuthorizationFailureEvent(eventObserver.authorizationFailureAsyncEvents);
        eventObserver.clearEvents();
        routingContext = (RoutingContext) authorizationFailureEvent.getEventProperties()
                .get(RoutingContext.class.getName());
        Assertions.assertNotNull(routingContext);

        // user has role 'http-upgrade-config' -> succeed
        TestIdentityController.resetRoles().add("user", "user", "http-upgrade-config");
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth(), httpUpgradeConfigEndpoint);
            client.waitForMessages(1);
            Assertions.assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            Assertions.assertEquals("hello", client.getMessages().get(1).toString());
        }
        assertEquals(1, eventObserver.authenticationSuccessEvents);
        assertEquals(1, eventObserver.authenticationSuccessAsyncEvents);
        assertEquals(0, eventObserver.authorizationFailureEvents);
        assertEquals(0, eventObserver.authorizationFailureAsyncEvents);
        assertEquals(1, eventObserver.authorizationSuccessEvents);
        assertEquals(1, eventObserver.authorizationSuccessAsyncEvents);
        assertEquals(0, eventObserver.authenticationFailureEvents);
        assertEquals(0, eventObserver.authenticationFailureAsyncEvents);
        assertAuthenticationSuccess();
        AuthorizationSuccessEvent authorizationSuccessEvent = assertAuthorizationSuccessEvent(
                eventObserver.authorizationSuccessEvents);
        routingContext = (RoutingContext) authorizationSuccessEvent.getEventProperties()
                .get(RoutingContext.class.getName());
        Assertions.assertNotNull(routingContext);
        authorizationSuccessEvent = assertAuthorizationSuccessEvent(eventObserver.authorizationSuccessAsyncEvents);
        routingContext = (RoutingContext) authorizationSuccessEvent.getEventProperties().get(RoutingContext.class.getName());
        Assertions.assertNotNull(routingContext);
    }

    @Test
    public void testOnTextMessageSecuredWithCheckerEvents() {
        TestIdentityController.resetRoles().add("user", "user");
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth(), onTextMessageAnnotationEndpoint);
            client.waitForMessages(1);
            Assertions.assertEquals("ready", client.getMessages().get(0).toString());

            // false -> permission checker denies access
            client.sendAndAwait("false");
            client.waitForMessages(2);
            Assertions.assertEquals("forbidden:user", client.getMessages().get(1).toString());

            // true -> permission checker grants access
            client.sendAndAwait("true");
            client.waitForMessages(3);
            String response = client.getMessages().get(2).toString();
            assertTrue(Boolean.parseBoolean(response));
        }

        assertAuthenticationSuccess();
        assertEquals(1, eventObserver.authenticationSuccessEvents);
        assertEquals(1, eventObserver.authenticationSuccessAsyncEvents);
        assertEquals(0, eventObserver.authenticationFailureEvents);
        assertEquals(0, eventObserver.authenticationFailureAsyncEvents);
        // the first message was denied
        assertEquals(1, eventObserver.authorizationFailureEvents);
        assertEquals(1, eventObserver.authorizationFailureAsyncEvents);
        // the second messages was permitted
        assertEquals(1, eventObserver.authorizationSuccessEvents);
        assertEquals(1, eventObserver.authorizationSuccessAsyncEvents);

        AuthorizationSuccessEvent authorizationSuccessEvent = assertAuthorizationSuccessEvent(
                eventObserver.authorizationSuccessEvents);
        String actualAuthZCtx = (String) authorizationSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT);
        Assertions.assertNotNull(actualAuthZCtx);
        Assertions.assertTrue(actualAuthZCtx.contains("PermissionSecurityCheck"));
        RoutingContext routingContext = HttpSecurityUtils
                .getRoutingContextAttribute(authorizationSuccessEvent.getSecurityIdentity());
        Assertions.assertNotNull(routingContext);
        String securedMethod = (String) authorizationSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.SECURED_METHOD_KEY);
        Assertions.assertTrue(securedMethod.contains("OnTextMessageEndpoint#echo"));
        authorizationSuccessEvent = assertAuthorizationSuccessEvent(eventObserver.authorizationSuccessAsyncEvents);
        actualAuthZCtx = (String) authorizationSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT);
        Assertions.assertNotNull(actualAuthZCtx);
        Assertions.assertTrue(actualAuthZCtx.contains("PermissionSecurityCheck"));
        routingContext = HttpSecurityUtils.getRoutingContextAttribute(authorizationSuccessEvent.getSecurityIdentity());
        Assertions.assertNotNull(routingContext);
        securedMethod = (String) authorizationSuccessEvent.getEventProperties()
                .get(AuthorizationSuccessEvent.SECURED_METHOD_KEY);
        Assertions.assertTrue(securedMethod.contains("OnTextMessageEndpoint#echo"));

        AuthorizationFailureEvent authorizationFailureEvent = assertAuthorizationFailureEvent(
                eventObserver.authorizationFailureEvents);
        actualAuthZCtx = (String) authorizationFailureEvent.getEventProperties()
                .get(AuthorizationFailureEvent.AUTHORIZATION_CONTEXT_KEY);
        Assertions.assertNotNull(actualAuthZCtx);
        Assertions.assertTrue(actualAuthZCtx.contains("PermissionSecurityCheck"));
        routingContext = HttpSecurityUtils.getRoutingContextAttribute(authorizationFailureEvent.getSecurityIdentity());
        Assertions.assertNotNull(routingContext);
        securedMethod = (String) authorizationFailureEvent.getEventProperties()
                .get(AuthorizationFailureEvent.SECURED_METHOD_KEY);
        Assertions.assertTrue(securedMethod.contains("OnTextMessageEndpoint#echo"));
        assertAuthorizationFailureEvent(eventObserver.authorizationFailureAsyncEvents);
    }

    private void assertAuthenticationSuccess() {
        AuthenticationSuccessEvent authenticationSuccessEvent = eventObserver.authenticationSuccessEvents.get(0);
        SecurityIdentity securityIdentity = authenticationSuccessEvent.getSecurityIdentity();
        Assertions.assertNotNull(securityIdentity);
        Assertions.assertEquals("user", securityIdentity.getPrincipal().getName());
        RoutingContext routingContext = (RoutingContext) authenticationSuccessEvent.getEventProperties()
                .get(RoutingContext.class.getName());
        Assertions.assertNotNull(routingContext);
    }

    private static WebSocketConnectOptions basicAuth() {
        return new WebSocketConnectOptions().addHeader(HttpHeaders.AUTHORIZATION.toString(),
                new UsernamePasswordCredentials("user", "user").applyHttpChallenge(null).toHttpAuthorization());
    }

    private static void assertEquals(int size, List<?> list) {
        await().untilAsserted(() -> Assertions.assertEquals(size, list.size()));
    }

    private static AuthorizationSuccessEvent assertAuthorizationSuccessEvent(List<AuthorizationSuccessEvent> events) {
        AuthorizationSuccessEvent event = events.get(0);
        SecurityIdentity securityIdentity = event.getSecurityIdentity();
        Assertions.assertNotNull(securityIdentity);
        Assertions.assertEquals("user", securityIdentity.getPrincipal().getName());
        return event;
    }

    private static AuthorizationFailureEvent assertAuthorizationFailureEvent(List<AuthorizationFailureEvent> events) {
        AuthorizationFailureEvent event = events.get(0);
        SecurityIdentity securityIdentity = event.getSecurityIdentity();
        Assertions.assertNotNull(securityIdentity);
        Assertions.assertEquals("user", securityIdentity.getPrincipal().getName());
        Throwable failure = event.getAuthorizationFailure();
        Assertions.assertInstanceOf(ForbiddenException.class, failure);
        return event;
    }

    @RolesAllowed("http-upgrade-annotation")
    @WebSocket(path = "/http-upgrade-annotation-endpoint", endpointId = "one-two-three")
    public static class HttpUpgradeAnnotationEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String echo) {
            return echo;
        }

    }

    @WebSocket(path = "/http-upgrade-config-endpoint")
    public static class HttpUpgradeConfigEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String echo) {
            return echo;
        }

    }

    @WebSocket(path = "/on-text-message-endpoint")
    public static class OnTextMessageEndpoint {

        @Inject
        SecurityIdentity currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @PermissionsAllowed("echo")
        @OnTextMessage
        String echo(boolean echo) {
            return Boolean.toString(echo);
        }

        @PermissionChecker("echo")
        boolean canCallEcho(boolean echo) {
            return echo;
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden:" + currentIdentity.getPrincipal().getName();
        }
    }

    @Singleton
    public static final class SecurityEventObserver {

        private final List<AuthenticationSuccessEvent> authenticationSuccessEvents = new CopyOnWriteArrayList<>();
        private final List<AuthenticationSuccessEvent> authenticationSuccessAsyncEvents = new CopyOnWriteArrayList<>();
        private final List<AuthenticationFailureEvent> authenticationFailureEvents = new CopyOnWriteArrayList<>();
        private final List<AuthenticationFailureEvent> authenticationFailureAsyncEvents = new CopyOnWriteArrayList<>();
        private final List<AuthorizationSuccessEvent> authorizationSuccessEvents = new CopyOnWriteArrayList<>();
        private final List<AuthorizationSuccessEvent> authorizationSuccessAsyncEvents = new CopyOnWriteArrayList<>();
        private final List<AuthorizationFailureEvent> authorizationFailureEvents = new CopyOnWriteArrayList<>();
        private final List<AuthorizationFailureEvent> authorizationFailureAsyncEvents = new CopyOnWriteArrayList<>();

        private void clearEvents() {
            authenticationSuccessEvents.clear();
            authenticationSuccessAsyncEvents.clear();
            authenticationFailureEvents.clear();
            authenticationFailureAsyncEvents.clear();
            authorizationSuccessEvents.clear();
            authorizationSuccessAsyncEvents.clear();
            authorizationFailureEvents.clear();
            authorizationFailureAsyncEvents.clear();
        }

        void observeAuthenticationSuccess(@Observes AuthenticationSuccessEvent event) {
            authenticationSuccessEvents.add(event);
        }

        void observeAuthenticationSuccessAsync(@ObservesAsync AuthenticationSuccessEvent event) {
            authenticationSuccessAsyncEvents.add(event);
        }

        void observeAuthenticationFailure(@Observes AuthenticationFailureEvent event) {
            authenticationFailureEvents.add(event);
        }

        void observeAuthenticationFailureAsync(@ObservesAsync AuthenticationFailureEvent event) {
            authenticationFailureAsyncEvents.add(event);
        }

        void observeAuthorizationSuccess(@Observes AuthorizationSuccessEvent event) {
            authorizationSuccessEvents.add(event);
        }

        void observeAuthorizationSuccessAsync(@ObservesAsync AuthorizationSuccessEvent event) {
            authorizationSuccessAsyncEvents.add(event);
        }

        void observeAuthorizationFailure(@Observes AuthorizationFailureEvent event) {
            authorizationFailureEvents.add(event);
        }

        void observeAuthorizationFailureAsync(@ObservesAsync AuthorizationFailureEvent event) {
            authorizationFailureAsyncEvents.add(event);
        }

    }
}
