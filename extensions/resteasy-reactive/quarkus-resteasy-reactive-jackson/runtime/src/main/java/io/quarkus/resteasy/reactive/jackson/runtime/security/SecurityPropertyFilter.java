package io.quarkus.resteasy.reactive.jackson.runtime.security;

import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.security.identity.SecurityIdentity;

public class SecurityPropertyFilter extends SimpleBeanPropertyFilter {

    static final String FILTER_ID = "securityFilter";

    @Override
    protected boolean include(PropertyWriter writer) {
        SecureField secureField = writer.getAnnotation(SecureField.class);
        if (secureField == null) {
            return super.include(writer);
        }

        ArcContainer container = Arc.container();
        if (container == null) {
            return false;
        }

        InstanceHandle<SecurityIdentity> instance = container.instance(SecurityIdentity.class);
        if (!instance.isAvailable()) {
            return false;
        }

        SecurityIdentity securityIdentity = instance.get();
        for (String role : secureField.rolesAllowed()) {
            if (securityIdentity.hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
