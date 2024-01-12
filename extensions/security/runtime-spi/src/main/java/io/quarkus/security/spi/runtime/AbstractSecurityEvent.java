package io.quarkus.security.spi.runtime;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;

public abstract class AbstractSecurityEvent implements SecurityEvent {

    protected final SecurityIdentity securityIdentity;
    protected final Map<String, Object> eventProperties;

    protected AbstractSecurityEvent(SecurityIdentity securityIdentity, Map<String, Object> eventProperties) {
        this.securityIdentity = securityIdentity;
        this.eventProperties = eventProperties == null ? Map.of() : Map.copyOf(eventProperties);
    }

    @Override
    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }

    @Override
    public Map<String, Object> getEventProperties() {
        return eventProperties;
    }

    protected static Map<String, Object> withProperties(String propertyKey, Object propertyValue,
            Map<String, Object> additionalProperties) {
        final Map<String, Object> result = new HashMap<>();
        if (propertyValue != null) {
            result.put(propertyKey, propertyValue);
        }
        if (additionalProperties != null && !additionalProperties.isEmpty()) {
            result.putAll(additionalProperties);
        }
        return result;
    }
}
