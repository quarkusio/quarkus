package io.quarkus.security.spi.runtime;

import java.util.Map;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.security.identity.SecurityIdentity;

public class SecurityEventHelper<S extends SecurityEvent, F extends SecurityEvent> {

    public static final AuthorizationFailureEvent AUTHORIZATION_FAILURE = new AuthorizationFailureEvent(null, null, null);
    public static final AuthorizationSuccessEvent AUTHORIZATION_SUCCESS = new AuthorizationSuccessEvent(null, null);
    public static final AuthenticationFailureEvent AUTHENTICATION_FAILURE = new AuthenticationFailureEvent(null, null);
    public static final AuthenticationSuccessEvent AUTHENTICATION_SUCCESS = new AuthenticationSuccessEvent(null, null);
    private final boolean fireEventOnSuccess;
    private final boolean fireEventOnFailure;
    private final Event<S> successEvent;
    private final Event<F> failureEvent;

    public SecurityEventHelper(Event<S> successEvent, Event<F> failureEvent, S successInstance, F failureInstance,
            BeanManager beanManager, boolean enabled) {
        if (enabled) {
            boolean fireAllSecurityEvents = !beanManager.resolveObserverMethods(new SecurityEvent() {
                @Override
                public SecurityIdentity getSecurityIdentity() {
                    return null;
                }

                @Override
                public Map<String, Object> getEventProperties() {
                    return Map.of();
                }
            }).isEmpty();
            if (fireAllSecurityEvents) {
                this.fireEventOnFailure = true;
                this.fireEventOnSuccess = true;
                this.successEvent = successEvent;
                this.failureEvent = failureEvent;
            } else {
                this.fireEventOnSuccess = !beanManager.resolveObserverMethods(successInstance).isEmpty();
                this.fireEventOnFailure = !beanManager.resolveObserverMethods(failureInstance).isEmpty();
                this.successEvent = this.fireEventOnSuccess ? successEvent : null;
                this.failureEvent = this.fireEventOnFailure ? failureEvent : null;
            }
        } else {
            this.fireEventOnSuccess = false;
            this.fireEventOnFailure = false;
            this.successEvent = null;
            this.failureEvent = null;
        }
    }

    public void fireSuccessEvent(S successInstance) {
        fire(successEvent, successInstance);
    }

    public void fireFailureEvent(F failureInstance) {
        fire(failureEvent, failureInstance);
    }

    public boolean fireEventOnSuccess() {
        return fireEventOnSuccess;
    }

    public boolean fireEventOnFailure() {
        return fireEventOnFailure;
    }

    public static <T extends SecurityEvent> void fire(Event<T> securityEvent, T event) {
        securityEvent.fire(event);
        securityEvent.fireAsync(event);
    }

    public static <T extends SecurityEvent> boolean isEventObserved(T event, BeanManager beanManager, boolean enabled) {
        if (enabled) {
            boolean fireAllSecurityEvents = !beanManager.resolveObserverMethods(new SecurityEvent() {
                @Override
                public SecurityIdentity getSecurityIdentity() {
                    return null;
                }

                @Override
                public Map<String, Object> getEventProperties() {
                    return Map.of();
                }
            }).isEmpty();
            return fireAllSecurityEvents || !beanManager.resolveObserverMethods(event).isEmpty();
        } else {
            return false;
        }
    }

    /**
     * Creates {@link SecurityEventHelper} initialized on first request.
     * This method should only be used when there is a risk the helper will be initialized during the static init phase.
     * During the runtime init phase, prefer the constructor.
     */
    public static <S extends SecurityEvent, F extends SecurityEvent> SecurityEventHelper<S, F> lazilyOf(Event<S> successEvent,
            Event<F> failureEvent, S successInstance, F failureInstance, BeanManager beanManager) {
        return new SecurityEventHelper<>(successEvent, failureEvent, successInstance, failureInstance, beanManager, true) {

            private volatile Boolean eventsDisabled = null;

            private boolean areEventsDisabled() {
                if (eventsDisabled == null) {
                    synchronized (this) {
                        if (eventsDisabled == null) {
                            this.eventsDisabled = !ConfigProvider.getConfig().getValue("quarkus.security.events.enabled",
                                    Boolean.class);
                        }
                    }
                }
                return eventsDisabled;
            }

            @Override
            public void fireSuccessEvent(S successInstance) {
                if (areEventsDisabled()) {
                    return;
                }
                super.fireSuccessEvent(successInstance);
            }

            @Override
            public void fireFailureEvent(F failureInstance) {
                if (areEventsDisabled()) {
                    return;
                }
                super.fireFailureEvent(failureInstance);
            }

            @Override
            public boolean fireEventOnSuccess() {
                if (areEventsDisabled()) {
                    return false;
                }
                return super.fireEventOnSuccess();
            }

            @Override
            public boolean fireEventOnFailure() {
                if (areEventsDisabled()) {
                    return false;
                }
                return super.fireEventOnFailure();
            }
        };
    }
}
