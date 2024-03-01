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
    private volatile InstanceHandle<RolesAllowedConfigExpStorage> rolesAllowedConfigExpStorage;

    private RolesAllowedConfigExpStorage getRolesAllowedConfigExpStorage(ArcContainer container) {
        if (rolesAllowedConfigExpStorage == null) {
            synchronized (this) {
                if (rolesAllowedConfigExpStorage == null) {
                    rolesAllowedConfigExpStorage = container.instance(RolesAllowedConfigExpStorage.class);
                }
            }
        }

        if (rolesAllowedConfigExpStorage.isAvailable()) {
            return rolesAllowedConfigExpStorage.get();
        } else {
            return null;
        }
    }

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
        RolesAllowedConfigExpStorage rolesConfigExpStorage = getRolesAllowedConfigExpStorage(container);
        for (String role : secureField.rolesAllowed()) {
            if (rolesConfigExpStorage != null) {
                // role config expression => resolved roles
                String[] roles = rolesConfigExpStorage.getRoles(role);
                if (roles != null) {
                    for (String r : roles) {
                        if (securityIdentity.hasRole(r)) {
                            return true;
                        }
                    }
                    continue;
                }
                // at this point, we know 'role' is not a configuration expression
            }
            if (securityIdentity.hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
