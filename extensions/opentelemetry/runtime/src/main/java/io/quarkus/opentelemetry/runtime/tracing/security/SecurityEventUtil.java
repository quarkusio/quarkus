package io.quarkus.opentelemetry.runtime.tracing.security;

import static io.quarkus.security.spi.runtime.AuthenticationFailureEvent.AUTHENTICATION_FAILURE_KEY;
import static io.quarkus.security.spi.runtime.AuthorizationFailureEvent.AUTHORIZATION_FAILURE_KEY;

import java.time.Instant;
import java.util.function.BiConsumer;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.arc.Arc;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

/**
 * Synthetic CDI observers for various {@link SecurityEvent} types configured during the build time use this util class
 * to export the events as the OpenTelemetry Span events, or authenticated user Span attributes.
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
     * Adds Span attributes describing authenticated user if the user is authenticated and CDI request context is active.
     * This will be true for example inside JAX-RS resources when the CDI request context is already setup and user code
     * creates a new Span.
     *
     * @param span valid and recording Span; must not be null
     */
    static void addEndUserAttributes(Span span) {
        if (Arc.container().requestContext().isActive()) {
            var currentVertxRequest = Arc.container().instance(CurrentVertxRequest.class).get();
            if (currentVertxRequest.getCurrent() != null) {
                addEndUserAttribute(currentVertxRequest.getCurrent(), span);
            }
        }
    }

    /**
     * Updates authenticated user Span attributes if the {@link SecurityIdentity} got augmented during authorization.
     *
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     *
     * @param event {@link AuthorizationFailureEvent}
     */
    public static void updateEndUserAttributes(AuthorizationFailureEvent event) {
        addEndUserAttribute(event.getSecurityIdentity(), getSpan());
    }

    /**
     * Updates authenticated user Span attributes if the {@link SecurityIdentity} got augmented during authorization.
     *
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     *
     * @param event {@link AuthorizationSuccessEvent}
     */
    public static void updateEndUserAttributes(AuthorizationSuccessEvent event) {
        addEndUserAttribute(event.getSecurityIdentity(), getSpan());
    }

    /**
     * If there is already valid recording {@link Span}, attributes describing authenticated user are added to it.
     *
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     *
     * @param event {@link AuthenticationSuccessEvent}
     */
    public static void addEndUserAttributes(AuthenticationSuccessEvent event) {
        addEndUserAttribute(event.getSecurityIdentity(), getSpan());
    }

    /**
     * Adds {@link SecurityEvent} as Span event.
     *
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
     * Adds {@link AuthenticationSuccessEvent} as Span event.
     *
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthenticationSuccessEvent event) {
        addEvent(AUTHN_SUCCESS_EVENT_NAME, attributesBuilder(event).build());
    }

    /**
     * Adds {@link AuthenticationFailureEvent} as Span event.
     *
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthenticationFailureEvent event) {
        addEvent(AUTHN_FAILURE_EVENT_NAME, attributesBuilder(event, AUTHENTICATION_FAILURE_KEY).build());
    }

    /**
     * Adds {@link AuthorizationSuccessEvent} as Span event.
     *
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthorizationSuccessEvent event) {
        addEvent(AUTHZ_SUCCESS_EVENT_NAME,
                withAuthorizationContext(event, attributesBuilder(event), AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT));
    }

    /**
     * Adds {@link AuthorizationFailureEvent} as Span event.
     *
     * WARNING: This method is called from synthetic method observer. Any renaming must be reflected in the TracerProcessor.
     */
    public static void addEvent(AuthorizationFailureEvent event) {
        addEvent(AUTHZ_FAILURE_EVENT_NAME, withAuthorizationContext(event, attributesBuilder(event, AUTHORIZATION_FAILURE_KEY),
                AuthorizationFailureEvent.AUTHORIZATION_CONTEXT_KEY));
    }

    /**
     * Adds {@link SecurityEvent} as Span event that is not authN/authZ success/failure.
     *
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
        Span span = getSpan();
        if (spanIsValidAndRecording(span)) {
            span.addEvent(eventName, attributes, Instant.now());
        }
    }

    private static AttributesBuilder attributesBuilder(SecurityEvent event, String failureKey) {
        if (event.getEventProperties().get(failureKey) instanceof Throwable failure) {
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

    /**
     * Adds Span attributes describing the authenticated user.
     *
     * @param event {@link RoutingContext}; must not be null
     * @param span valid recording Span; must not be null
     */
    private static void addEndUserAttribute(RoutingContext event, Span span) {
        if (event.user() instanceof QuarkusHttpUser user) {
            addEndUserAttribute(user.getSecurityIdentity(), span);
        }
    }

    /**
     * Adds End User attributes to the {@code span}. Only authenticated user is added to the {@link Span}.
     * Anonymous identity is ignored as it does not represent authenticated user.
     * Passed {@code securityIdentity} is attached to the {@link Context} so that we recognize when identity changes.
     *
     * @param securityIdentity SecurityIdentity
     * @param span Span
     */
    private static void addEndUserAttribute(SecurityIdentity securityIdentity, Span span) {
        if (securityIdentity != null && !securityIdentity.isAnonymous() && spanIsValidAndRecording(span)) {
            span.setAllAttributes(Attributes.of(
                    SemanticAttributes.ENDUSER_ID,
                    securityIdentity.getPrincipal().getName(),
                    SemanticAttributes.ENDUSER_ROLE,
                    getRoles(securityIdentity)));
        }
    }

    private static String getRoles(SecurityIdentity securityIdentity) {
        try {
            return securityIdentity.getRoles().toString();
        } catch (UnsupportedOperationException e) {
            // getting roles is not supported when the identity is enhanced by custom jakarta.ws.rs.core.SecurityContext
            return "";
        }
    }

    private static Span getSpan() {
        if (Arc.container().requestContext().isActive()) {
            return Arc.container().select(Span.class).get();
        } else {
            return Span.current();
        }
    }

    private static boolean spanIsValidAndRecording(Span span) {
        return span.isRecording() && span.getSpanContext().isValid();
    }
}
