package io.quarkus.security.test.cdi.events;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithNoSecurityAnnotations;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithSecurityAnnotations;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledCDISecurityEventTest {

    @Inject
    @Named(BeanWithSecurityAnnotations.NAME)
    BeanWithSecurityAnnotations beanWithSecurityAnnotations;

    @Inject
    SecurityEventObserver observer;

    @Inject
    AsyncAuthZFailureEventObserver asyncAuthZFailureObserver;

    @Inject
    Event<SecurityEvent> producer;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithNoSecurityAnnotations.class, BeanWithSecurityAnnotations.class,
                            SecurityTestUtils.class, IdentityMock.class, SecurityEventObserver.class,
                            AsyncAuthZFailureEventObserver.class)
                    .addAsResource(new StringAsset("quarkus.security.events.enabled=false\n"), "application.properties"));

    @Test
    public void testNoOpSecurityEventProducer() {
        assertSuccess(beanWithSecurityAnnotations::restricted, "accessibleForAdminOnly", ADMIN);
        assertEquals(0, observer.getObserverEvents().size());
        assertFailureFor(beanWithSecurityAnnotations::restricted, UnauthorizedException.class, ANONYMOUS);
        assertEquals(0, observer.getObserverEvents().size());

        // prove firing same events directly still works
        producer.fireAsync(new AuthorizationFailureEvent(null, null, null));
        producer.fire(new AuthorizationFailureEvent(null, null, null));
        assertEquals(1, observer.getObserverEvents().size());
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(1, asyncAuthZFailureObserver.getObserverEvents().size()));
    }

}
