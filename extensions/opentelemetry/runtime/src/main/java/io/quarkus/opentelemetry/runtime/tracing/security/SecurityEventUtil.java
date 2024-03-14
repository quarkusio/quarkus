package io.quarkus.opentelemetry.runtime.tracing.security;

import static io.quarkus.security.spi.runtime.AuthenticationFailureEvent.AUTHENTICATION_FAILURE_KEY;
import static io.quarkus.security.spi.runtime.AuthorizationFailureEvent.AUTHORIZATION_FAILURE_KEY;

import java.time.Instant;
import java.util.function.BiConsumer;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.quarkus.arc.Arc;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;

/**
 * Synthetic CDI observers for various {@link SecurityEvent} types configured during the build time use this util class
 * to export the events as the OpenTelemetry Span events.
 */
public final class SecurityEventUtil {
    public static final String QUARKUS_SECURITY_NAMESPACE = "quarkus.security.";
    public static final String AUTHN_SUCCESS_EVENT_NAME = QUARKUS_SECURITY_NAMESPACE + "authentication.success";
    public static final String AUTHN_FAILURE_EVENT_NAME = QUARKUS_SECURITY_NAMESPACE + "authentication.failure";
    public static final String AUTHZ_SUCCESS_EVENT_NAME = QUARKUS_SECURITY_NAMESPACE + "authorization.success";
    public static final String AUTHZ_FAILURE_EVENT_NAME = QUARKUS_SECURITY_NAMESPACE + "authorization.failure";
    public static final String OTHER_EVENT_NAME = QUARKUS_SECURITY_NAMESPACE + "other";
    public static final String SECURITY_IDENTITY_PRINCIPAL = QUARKUS_SECURITY_NAMESPACE + "identity.principal";
    public static final String SECURITY_IDENTITY_IS_ANONYMOUS = QUARKUS_SECURITY_NAMESPACE + "identity.anonymous";
    public static final String QUARKUS_SECURITY_OTHER_EVENTS_NAMESPACE = QUARKUS_SECURITY_NAMESPACE + "other.";
    public static final String FAILURE_NAME = QUARKUS_SECURITY_NAMESPACE + "failure.name";
    public static final String AUTHORIZATION_CONTEXT = QUARKUS_SECURITY_NAMESPACE + "authorization.context";

    private SecurityEventUtil() {
        // UTIL CLASS
    }

    /**
     * Adds {@link SecurityEvent} as Span event.
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addAllEvents(SecurityEvent event) {
        if (event instanceof AuthenticationSuccessEvent e) {
            addEvent(e);
        } else if (event instanceof AuthenticationFailureEvent e) {
            addEvent(e);
        } else if (event instanceof AuthorizationSuccessEvent e) {
            addEvent(e);
        } else if (event instanceof AuthorizationFailureEvent e) {
            addEvent(e);
        } else {
            addOtherEventInternal(event);
        }
    }

    /**
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthenticationSuccessEvent event) {
        addEvent(AUTHN_SUCCESS_EVENT_NAME, attributesBuilder(event).build());
    }

    /**
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthenticationFailureEvent event) {
        addEvent(AUTHN_FAILURE_EVENT_NAME, attributesBuilder(event, AUTHENTICATION_FAILURE_KEY).build());
    }

    /**
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthorizationSuccessEvent event) {
        addEvent(AUTHZ_SUCCESS_EVENT_NAME,
                withAuthorizationContext(event, attributesBuilder(event), AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT));
    }

    /**
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthorizationFailureEvent event) {
        addEvent(AUTHZ_FAILURE_EVENT_NAME, withAuthorizationContext(event, attributesBuilder(event, AUTHORIZATION_FAILURE_KEY),
                AuthorizationFailureEvent.AUTHORIZATION_CONTEXT_KEY));
    }

    /**
     * Adds {@link SecurityEvent} as Span event that is not authN/authZ success/failure.
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(SecurityEvent event) {
        if (!(event instanceof AuthenticationSuccessEvent || event instanceof AuthenticationFailureEvent
                || event instanceof AuthorizationSuccessEvent || event instanceof AuthorizationFailureEvent)) {
            addOtherEventInternal(event);
        }
    }

    private static void addOtherEventInternal(SecurityEvent event) {
        var builder = attributesBuilder(event);
        // add all event properties that are string, for example OIDC authentication server URL
        event.getEventProperties().forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                if (value instanceof String str) {
                    builder.put(QUARKUS_SECURITY_OTHER_EVENTS_NAMESPACE + key, str);
                }
            }
        });
        addEvent(OTHER_EVENT_NAME, builder.build());
    }

    private static void addEvent(String eventName, Attributes attributes) {
        Span span = Arc.container().select(Span.class).get();
        if (span.getSpanContext().isValid()) {
            span.addEvent(eventName, attributes, Instant.now());
        }
    }

    private static AttributesBuilder attributesBuilder(SecurityEvent event, String failureKey) {
        Throwable failure = (Throwable) event.getEventProperties().get(failureKey);
        if (failure != null) {
            return attributesBuilder(event).put(FAILURE_NAME, failure.getClass().getName());
        }
        return attributesBuilder(event);
    }

    private static AttributesBuilder attributesBuilder(SecurityEvent event) {
        var builder = Attributes.builder();

        SecurityIdentity identity = event.getSecurityIdentity();
        if (identity != null) {
            builder.put(SECURITY_IDENTITY_IS_ANONYMOUS, identity.isAnonymous());
            if (identity.getPrincipal() != null) {
                builder.put(SECURITY_IDENTITY_PRINCIPAL, identity.getPrincipal().getName());
            }
        }

        return builder;
    }

    private static Attributes withAuthorizationContext(SecurityEvent event, AttributesBuilder builder, String contextKey) {
        if (event.getEventProperties().containsKey(contextKey)) {
            builder.put(AUTHORIZATION_CONTEXT, (String) event.getEventProperties().get(contextKey));
        }
        return builder.build();
    }
}
