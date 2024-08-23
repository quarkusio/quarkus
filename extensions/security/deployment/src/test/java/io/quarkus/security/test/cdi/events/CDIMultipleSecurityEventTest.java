package io.quarkus.security.test.cdi.events;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithNoSecurityAnnotations;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithSecurityAnnotations;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class CDIMultipleSecurityEventTest {

    @Inject
    @Named(BeanWithSecurityAnnotations.NAME)
    BeanWithSecurityAnnotations beanWithSecurityAnnotations;

    @Inject
    AuthZFailureEventObserver authZFailureEventObserver;

    @Inject
    SecurityEventObserver securityEventObserver;

    @Inject
    CustomEventService customEventService;

    @Inject
    CustomEventObserver customEventObserver;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithNoSecurityAnnotations.class, BeanWithSecurityAnnotations.class,
                            SecurityTestUtils.class, IdentityMock.class, AuthZFailureEventObserver.class,
                            SecurityEventObserver.class, CustomSecurityEvent.class, CustomEventService.class,
                            CustomEventObserver.class));

    @BeforeEach
    public void beforeEach() {
        securityEventObserver.getObserverEvents().clear();
        authZFailureEventObserver.getObserverEvents().clear();
        customEventObserver.getObserverEvents().clear();
    }

    @Test
    public void testBothSecurityAndAuthorizationObserversNotified() {
        // one event is fired, 2 observers are notified
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(1, securityEventObserver.getObserverEvents().size());
        assertTrue(securityEventObserver.getObserverEvents().get(0) instanceof AuthorizationSuccessEvent);
        assertEquals(0, authZFailureEventObserver.getObserverEvents().size());
        assertEquals(0, customEventObserver.getObserverEvents().size());

        assertFailureFor(beanWithSecurityAnnotations::restricted, UnauthorizedException.class, ANONYMOUS);
        assertEquals(1, authZFailureEventObserver.getObserverEvents().size());
        assertEquals(2, securityEventObserver.getObserverEvents().size());
        assertEquals(0, customEventObserver.getObserverEvents().size());
        SecurityEvent securityEvent = securityEventObserver.getObserverEvents().get(1);
        AuthorizationFailureEvent authZEvent = authZFailureEventObserver.getObserverEvents().get(0);
        assertEquals(securityEvent, authZEvent);
    }

    @Test
    public void testCustomSecurityEventAndAuthZEvents() {
        // no event is fired as admin user has required role
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(1, securityEventObserver.getObserverEvents().size());
        assertTrue(securityEventObserver.getObserverEvents().get(0) instanceof AuthorizationSuccessEvent);
        assertEquals(0, authZFailureEventObserver.getObserverEvents().size());
        assertEquals(0, customEventObserver.getObserverEvents().size());

        // custom event is fired, but no authZ event is fired as admin has required role
        assertSuccess(customEventService::fireBothCustomAndAuthZEvent, "accessibleForAdminOnly", ADMIN);
        assertEquals(0, authZFailureEventObserver.getObserverEvents().size());
        assertEquals(3, securityEventObserver.getObserverEvents().size());
        assertEquals(1, customEventObserver.getObserverEvents().size());
        customEventObserver.getObserverEvents().clear();
        securityEventObserver.getObserverEvents().clear();

        // 2 events are fired, 3 observers are notified
        assertFailureFor(customEventService::fireBothCustomAndAuthZEvent, UnauthorizedException.class, ANONYMOUS);
        assertEquals(1, authZFailureEventObserver.getObserverEvents().size());
        assertEquals(1, customEventObserver.getObserverEvents().size());
        assertEquals(2, securityEventObserver.getObserverEvents().size());
        AuthorizationFailureEvent authZEvent = authZFailureEventObserver.getObserverEvents().get(0);
        CustomSecurityEvent customEvent = customEventObserver.getObserverEvents().get(0);
        assertTrue(securityEventObserver.getObserverEvents().stream().anyMatch(authZEvent::equals));
        assertTrue(securityEventObserver.getObserverEvents().stream().anyMatch(customEvent::equals));
    }

    @Singleton
    public static class CustomEventService {

        @Inject
        Event<SecurityEvent> event;

        @Inject
        @Named(BeanWithSecurityAnnotations.NAME)
        BeanWithSecurityAnnotations beanWithSecurityAnnotations;

        String fireBothCustomAndAuthZEvent() {
            event.fire(new CustomSecurityEvent());
            return beanWithSecurityAnnotations.restricted();
        }
    }

}
