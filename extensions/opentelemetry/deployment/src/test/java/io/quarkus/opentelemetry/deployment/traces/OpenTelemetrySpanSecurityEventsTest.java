package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.tracing.security.SecurityEventUtil;
import io.quarkus.security.spi.runtime.AbstractSecurityEvent;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetrySpanSecurityEventsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class, EventsResource.class,
                            CustomSecurityEvent.class)
                    .addAsResource(new StringAsset("""
                            quarkus.otel.security-events.enabled=true
                            quarkus.otel.metrics.exporter=none
                            quarkus.otel.security-events.event-types=AUTHENTICATION_SUCCESS,AUTHORIZATION_SUCCESS,OTHER
                            """), "application.properties"));

    @Inject
    TestSpanExporter spanExporter;

    @Test
    public void testSecurityEventTypes() {
        spanExporter.assertSpanCount(0);
        RestAssured.post("/events");
        spanExporter.assertSpanCount(1);
        var events = spanExporter.getFinishedSpanItems(1).get(0).getEvents();
        assertEquals(3, events.size());
        assertTrue(events.stream().anyMatch(ed -> SecurityEventUtil.AUTHN_SUCCESS_EVENT_NAME.equals(ed.getName())));
        assertTrue(events.stream().anyMatch(ed -> SecurityEventUtil.AUTHZ_SUCCESS_EVENT_NAME.equals(ed.getName())));
        assertTrue(events.stream().anyMatch(ed -> SecurityEventUtil.OTHER_EVENT_NAME.equals(ed.getName())));
        var event = events.stream().filter(s -> SecurityEventUtil.OTHER_EVENT_NAME.equals(s.getName())).findFirst()
                .orElse(null);
        assertNotNull(event);
        assertEquals(1, event.getAttributes().size());
        assertEquals(CustomSecurityEvent.CUSTOM_VALUE, event.getAttributes().get(AttributeKey
                .stringKey(SecurityEventUtil.QUARKUS_SECURITY_OTHER_EVENTS_NAMESPACE + CustomSecurityEvent.CUSTOM_KEY)));
    }

    public static class CustomSecurityEvent extends AbstractSecurityEvent {
        private static final String CUSTOM_KEY = "custom-key";
        private static final String CUSTOM_VALUE = "custom-value";

        protected CustomSecurityEvent() {
            super(null, Map.of(CUSTOM_KEY, CUSTOM_VALUE));
        }
    }

    @Path("/events")
    public static class EventsResource {

        @Inject
        Event<AuthenticationSuccessEvent> authenticationSuccessEvent;

        @Inject
        Event<AuthenticationFailureEvent> authenticationFailureEvent;

        @Inject
        Event<AuthorizationFailureEvent> authorizationFailureEvent;

        @Inject
        Event<AuthorizationSuccessEvent> authorizationSuccessEvent;

        @Inject
        Event<CustomSecurityEvent> customEvent;

        @POST
        public void fire() {
            authenticationSuccessEvent.fire(new AuthenticationSuccessEvent(null, null));
            authenticationFailureEvent.fire(new AuthenticationFailureEvent(new ForbiddenException(), null));
            authorizationSuccessEvent.fire(new AuthorizationSuccessEvent(null, null));
            authorizationFailureEvent.fire(new AuthorizationFailureEvent(null, new ForbiddenException(), "endpoint"));
            customEvent.fire(new CustomSecurityEvent());
        }
    }
}
