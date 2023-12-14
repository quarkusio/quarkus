package io.quarkus.security.test.cdi.events;

import static io.quarkus.security.spi.runtime.AuthorizationFailureEvent.AUTHORIZATION_CONTEXT_KEY;
import static io.quarkus.security.spi.runtime.AuthorizationFailureEvent.AUTHORIZATION_FAILURE_KEY;
import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithNoSecurityAnnotations;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithSecurityAnnotations;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class CDIAuthorizationFailureEventTest {

    @Inject
    @Named(BeanWithSecurityAnnotations.NAME)
    BeanWithSecurityAnnotations beanWithSecurityAnnotations;

    @Inject
    AuthZFailureEventObserver observer;

    @Inject
    AsyncAuthZFailureEventObserver asyncObserver;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithNoSecurityAnnotations.class, BeanWithSecurityAnnotations.class,
                            SecurityTestUtils.class, IdentityMock.class, AuthZFailureEventObserver.class,
                            AsyncAuthZFailureEventObserver.class));

    @BeforeEach
    public void beforeEach() {
        observer.getObserverEvents().clear();
        asyncObserver.getObserverEvents().clear();
    }

    @Test
    public void testAuthorizationEventObservedByAuthZEvent() {
        // bean method has restricted access with @RolesAllowed
        // AuthorizationFailureEvent is fired for anonymous identity doesn't have required roles
        // observer observes AuthorizationFailureEvent
        // all events are fired for same failed SecurityCheck and same invoked secured method

        // first assert no event is fired when security check pass
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(0, observer.getObserverEvents().size());

        // then assert event is fired for anonymous user can't access bean method with @RolesAllowed("admin") annotation
        assertFailureFor(beanWithSecurityAnnotations::restricted, UnauthorizedException.class, ANONYMOUS);
        assertEquals(1, observer.getObserverEvents().size());
        assertAsyncObserved(1);
        AuthorizationFailureEvent firstEvent = observer.getObserverEvents().get(0);
        SecurityIdentity securityIdentity = firstEvent.getSecurityIdentity();
        assertNotNull(securityIdentity);
        assertTrue(securityIdentity.isAnonymous());
        Throwable failure = firstEvent.getAuthorizationFailure();
        assertNotNull(failure);
        assertTrue(failure instanceof UnauthorizedException);
        assertEquals(failure, firstEvent.getEventProperties().get(AUTHORIZATION_FAILURE_KEY));
        String authZContext = firstEvent.getAuthorizationContext();
        assertNotNull(securityIdentity);
        assertEquals(RolesAllowedCheck.class.getName(), authZContext);
        assertEquals(authZContext, firstEvent.getEventProperties().get(AUTHORIZATION_CONTEXT_KEY));

        // make sure no new event is reported for passing check
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(1, observer.getObserverEvents().size());
        assertAsyncObserved(1);

        // assert second event is fired as authenticated user doesn't have 'admin' role
        assertFailureFor(beanWithSecurityAnnotations::restricted, ForbiddenException.class, USER);
        assertEquals(2, observer.getObserverEvents().size());
        assertAsyncObserved(2);
        AuthorizationFailureEvent secondEvent = observer.getObserverEvents().get(1);
        securityIdentity = secondEvent.getSecurityIdentity();
        assertNotNull(securityIdentity);
        assertFalse(securityIdentity.isAnonymous());
        assertEquals("user", securityIdentity.getPrincipal().getName());
        failure = secondEvent.getAuthorizationFailure();
        assertNotNull(failure);
        assertTrue(failure instanceof ForbiddenException);
        assertEquals(failure, secondEvent.getEventProperties().get(AUTHORIZATION_FAILURE_KEY));
        authZContext = secondEvent.getAuthorizationContext();
        assertNotNull(securityIdentity);
        assertEquals(RolesAllowedCheck.class.getName(), authZContext);
        assertEquals(authZContext, secondEvent.getEventProperties().get(AUTHORIZATION_CONTEXT_KEY));
    }

    @Test
    public void testEventForRolesAllowed() {
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(0, observer.getObserverEvents().size());
        assertFailureFor(beanWithSecurityAnnotations::restricted, ForbiddenException.class, USER);
        assertEquals(1, observer.getObserverEvents().size());
        assertAsyncObserved(1);
    }

    @Test
    public void testEventForAuthenticated() {
        assertSuccess(beanWithSecurityAnnotations::authenticated, "authenticated", USER);
        assertEquals(0, observer.getObserverEvents().size());
        assertFailureFor(beanWithSecurityAnnotations::authenticated, UnauthorizedException.class, ANONYMOUS);
        assertEquals(1, observer.getObserverEvents().size());
        assertAsyncObserved(1);
    }

    @Test
    public void testEventForDenyAll() {
        assertFailureFor(beanWithSecurityAnnotations::deny, ForbiddenException.class, USER);
        assertEquals(1, observer.getObserverEvents().size());
        assertAsyncObserved(1);
        assertFailureFor(beanWithSecurityAnnotations::deny, UnauthorizedException.class, ANONYMOUS);
        assertEquals(2, observer.getObserverEvents().size());
        assertAsyncObserved(2);
    }

    @Test
    public void testNoEventForPermitAll() {
        assertSuccess(beanWithSecurityAnnotations::allowed, "allowed", ANONYMOUS);
        assertEquals(0, observer.getObserverEvents().size());
        assertAsyncObserved(0);
    }

    private void assertAsyncObserved(int eventCount) {
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(eventCount, asyncObserver.getObserverEvents().size()));
    }

}
