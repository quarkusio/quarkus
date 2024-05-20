package io.quarkus.security.spi.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    protected static String toString(MethodDescription methodDescription) {
        Objects.requireNonNull(methodDescription);
        return methodDescription.getClassName() + "#" + methodDescription.getMethodName();
    }

    protected static Map<String, Object> withProperties(String propertyKey, Object propertyValue,
            Map<String, Object> additionalProperties) {

        final HashMap<String, Object> result;
        if (additionalProperties instanceof HashMap<String, Object> additionalPropertiesHashMap) {
            // do not recreate map when multiple props are added
            result = additionalPropertiesHashMap;
        } else {
            result = new HashMap<>();
            if (additionalProperties != null && !additionalProperties.isEmpty()) {
                result.putAll(additionalProperties);
            }
        }

        if (propertyValue != null) {
            result.put(propertyKey, propertyValue);
        }
        return result;
    }
}
