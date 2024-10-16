package io.quarkus.security.test.cdi.events;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithNoSecurityAnnotations;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithSecurityAnnotations;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class AsyncCDISecurityEventTest {

    @Inject
    @Named(BeanWithSecurityAnnotations.NAME)
    BeanWithSecurityAnnotations beanWithSecurityAnnotations;

    @Inject
    SecurityEventObserver observer;

    @Inject
    AsyncSecurityEventObserver asyncObserver;

    @Inject
    AsyncAuthZSuccessEventObserver asyncAuthZSuccessEventObserver;

    @Inject
    AsyncAuthZFailureEventObserver asyncAuthZFailureObserver;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithNoSecurityAnnotations.class, BeanWithSecurityAnnotations.class,
                            SecurityTestUtils.class, IdentityMock.class, SecurityEventObserver.class,
                            AsyncSecurityEventObserver.class, AsyncAuthZFailureEventObserver.class,
                            AsyncAuthZSuccessEventObserver.class));

    @BeforeEach
    public void beforeEach() {
        observer.getObserverEvents().clear();
        asyncObserver.getObserverEvents().clear();
        asyncAuthZFailureObserver.getObserverEvents().clear();
        asyncAuthZSuccessEventObserver.getObserverEvents().clear();
    }

    @Test
    public void testAuthSuccessAsyncObserverNotified() {
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(1, observer.getObserverEvents().size());
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, asyncObserver.getObserverEvents().size()));
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, asyncAuthZSuccessEventObserver.getObserverEvents().size()));
        assertEquals(0, asyncAuthZFailureObserver.getObserverEvents().size());
    }

    @Test
    public void testAuthFailureAsyncObserverNotified() {
        assertFailureFor(beanWithSecurityAnnotations::restricted, UnauthorizedException.class, ANONYMOUS);
        assertEquals(1, observer.getObserverEvents().size());
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, asyncObserver.getObserverEvents().size()));
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, asyncAuthZFailureObserver.getObserverEvents().size()));
        AuthorizationFailureEvent event = asyncAuthZFailureObserver.getObserverEvents().get(0);
        assertTrue(event.getAuthorizationFailure() instanceof UnauthorizedException);
        assertNotNull(event.getSecurityIdentity());
        assertTrue(event.getSecurityIdentity().isAnonymous());
        assertEquals(RolesAllowedCheck.class.getName(), event.getAuthorizationContext());
        assertEquals(0, asyncAuthZSuccessEventObserver.getObserverEvents().size());
    }

}
