package io.quarkus.security.test.cdi.events;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityEvent;

public class CustomSecurityEvent implements SecurityEvent {

    private static final String CUSTOM_KEY = "custom_key";
    private static final String CUSTOM_VALUE = "custom_value";

    CustomSecurityEvent() {
    }

    @Override
    public SecurityIdentity getSecurityIdentity() {
        return QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal("custom")).build();
    }

    @Override
    public Map<String, Object> getEventProperties() {
        return Map.of(CUSTOM_KEY, CUSTOM_VALUE);
    }
}
