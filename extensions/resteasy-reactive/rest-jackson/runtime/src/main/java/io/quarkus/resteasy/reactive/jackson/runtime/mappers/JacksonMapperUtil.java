package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.resteasy.reactive.jackson.runtime.security.RolesAllowedConfigExpStorage;
import io.quarkus.security.identity.SecurityIdentity;

public class JacksonMapperUtil {

    public static boolean includeSecureField(String[] rolesAllowed) {
        SecurityIdentity securityIdentity = RolesAllowedHolder.SECURITY_IDENTITY;
        if (securityIdentity == null) {
            return false;
        }

        RolesAllowedConfigExpStorage rolesConfigExpStorage = RolesAllowedHolder.ROLES_ALLOWED_CONFIG_EXP_STORAGE;
        for (String role : rolesAllowed) {
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

    private static class RolesAllowedHolder {

        private static final ArcContainer ARC_CONTAINER = Arc.container();

        private static final SecurityIdentity SECURITY_IDENTITY = createSecurityIdentity();

        private static final RolesAllowedConfigExpStorage ROLES_ALLOWED_CONFIG_EXP_STORAGE = createRolesAllowedConfigExpStorage();

        private static SecurityIdentity createSecurityIdentity() {
            if (ARC_CONTAINER == null) {
                return null;
            }
            InstanceHandle<SecurityIdentity> instance = ARC_CONTAINER.instance(SecurityIdentity.class);
            return instance.isAvailable() ? instance.get() : null;
        }

        private static RolesAllowedConfigExpStorage createRolesAllowedConfigExpStorage() {
            if (ARC_CONTAINER == null) {
                return null;
            }
            InstanceHandle<RolesAllowedConfigExpStorage> rolesAllowedConfigExpStorage = ARC_CONTAINER
                    .instance(RolesAllowedConfigExpStorage.class);
            return rolesAllowedConfigExpStorage.isAvailable() ? rolesAllowedConfigExpStorage.get() : null;
        }
    }

    public static JavaType[] getGenericsJavaTypes(DeserializationContext context, BeanProperty property) {
        JavaType wrapperType = property != null ? property.getType() : context.getContextualType();
        JavaType[] valueTypes = new JavaType[wrapperType.containedTypeCount()];
        for (int i = 0; i < valueTypes.length; i++) {
            valueTypes[i] = wrapperType.containedType(0);
        }
        return valueTypes;
    }
}
