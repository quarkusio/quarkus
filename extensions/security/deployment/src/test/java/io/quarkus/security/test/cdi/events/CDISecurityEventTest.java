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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithNoSecurityAnnotations;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithSecurityAnnotations;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class CDISecurityEventTest {

    @Inject
    @Named(BeanWithSecurityAnnotations.NAME)
    BeanWithSecurityAnnotations beanWithSecurityAnnotations;

    @Inject
    SecurityEventObserver observer;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithNoSecurityAnnotations.class, BeanWithSecurityAnnotations.class,
                            SecurityTestUtils.class, IdentityMock.class, SecurityEventObserver.class));

    @BeforeEach
    public void beforeEach() {
        observer.getObserverEvents().clear();
    }

    @Test
    public void testAuthZEventsObservedBySecurityEvent() {
        // bean method has restricted access with @RolesAllowed
        // AuthorizationFailureEvent is fired for anonymous identity doesn't have required roles
        // observer observes SecurityEvent
        // all events are fired for same failed SecurityCheck and same invoked secured method

        // first assert auth success event is fired when security check pass
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(1, observer.getObserverEvents().size());
        assertTrue(observer.getObserverEvents().get(0) instanceof AuthorizationSuccessEvent);

        // then assert event is fired for anonymous user can't access bean method with @RolesAllowed("admin") annotation
        assertFailureFor(beanWithSecurityAnnotations::restricted, UnauthorizedException.class, ANONYMOUS);
        assertEquals(2, observer.getObserverEvents().size());
        SecurityEvent firstEvent = observer.getObserverEvents().get(1);
        SecurityIdentity securityIdentity = firstEvent.getSecurityIdentity();
        assertNotNull(securityIdentity);
        assertTrue(securityIdentity.isAnonymous());
        if (firstEvent instanceof AuthorizationFailureEvent authorizationFailureEvent) {
            Throwable failure = authorizationFailureEvent.getAuthorizationFailure();
            assertNotNull(failure);
            assertTrue(failure instanceof UnauthorizedException);
            assertEquals(failure, firstEvent.getEventProperties().get(AUTHORIZATION_FAILURE_KEY));
            String authZContext = authorizationFailureEvent.getAuthorizationContext();
            assertNotNull(securityIdentity);
            assertEquals(RolesAllowedCheck.class.getName(), authZContext);
            assertEquals(authZContext, firstEvent.getEventProperties().get(AUTHORIZATION_CONTEXT_KEY));
        } else {
            Assertions.fail("Expected AuthorizationEvent event, got: " + firstEvent);
        }

        // make sure auth success event is reported for passing check
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(3, observer.getObserverEvents().size());
        assertTrue(observer.getObserverEvents().get(0) instanceof AuthorizationSuccessEvent);

        // assert second event is fired as authenticated user doesn't have 'admin' role
        assertFailureFor(beanWithSecurityAnnotations::restricted, ForbiddenException.class, USER);
        assertEquals(4, observer.getObserverEvents().size());
        SecurityEvent secondEvent = observer.getObserverEvents().get(3);
        securityIdentity = firstEvent.getSecurityIdentity();
        assertNotNull(securityIdentity);
        assertFalse(securityIdentity.isAnonymous());
        assertEquals("user", securityIdentity.getPrincipal().getName());
        if (secondEvent instanceof AuthorizationFailureEvent authorizationFailureEvent) {
            Throwable failure = authorizationFailureEvent.getAuthorizationFailure();
            assertNotNull(failure);
            assertTrue(failure instanceof ForbiddenException);
            assertEquals(failure, secondEvent.getEventProperties().get(AUTHORIZATION_FAILURE_KEY));
            String securityCheckClassName = authorizationFailureEvent.getAuthorizationContext();
            assertNotNull(securityIdentity);
            assertEquals(RolesAllowedCheck.class.getName(), securityCheckClassName);
            assertEquals(securityCheckClassName, secondEvent.getEventProperties().get(AUTHORIZATION_CONTEXT_KEY));
        } else {
            Assertions.fail("Expected AuthorizationEvent event, got: " + secondEvent);
        }
    }

    @Test
    public void testPermitSecurityCheckAuthSuccessEvent() {
        assertSuccess(beanWithSecurityAnnotations::allowed, "allowed", ANONYMOUS);
        assertEquals(1, observer.getObserverEvents().size());
        SecurityEvent event = observer.getObserverEvents().get(0);
        if (event instanceof AuthorizationSuccessEvent successEvent) {
            assertEquals(PermitAllCheck.class.getName(),
                    successEvent.getEventProperties().get(AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT));
            // SecurityIdentity is null because authentication is not required for the PermitAll check
            assertNull(successEvent.getSecurityIdentity());
        } else {
            Assertions.fail("AuthorizationSuccessEvent was not observed for @PermitAll annotation");
        }
    }

}
