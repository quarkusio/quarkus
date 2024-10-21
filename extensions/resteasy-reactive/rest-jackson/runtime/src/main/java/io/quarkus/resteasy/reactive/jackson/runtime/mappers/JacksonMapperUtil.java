package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;

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

    /**
     * Determine the root type that should be used for serialization of generic types.
     * Returns the appropriate root type or {@code null} if default serialization should be used.
     */
    public static JavaType getGenericRootType(Type genericType, ObjectWriter defaultWriter) {
        // Jackson needs additional type information when serializing generic types, as discussed here:
        // https://github.com/FasterXML/jackson-databind/issues/336 and https://github.com/FasterXML/jackson-databind/issues/23
        // Parts of the code were taken from org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider
        // which was used in quarkus-resteasy to handle this situation.
        JavaType rootType = null;
        if (genericType != null) {
            /*
             * 10-Jan-2011, tatu: as per [JACKSON-456], it's not safe to just force root
             * type since it prevents polymorphic type serialization. Since we really
             * just need this for generics, let's only use generic type if it's truly
             * generic.
             */
            if (genericType.getClass() != Class.class) {
                rootType = defaultWriter.getTypeFactory().constructType(genericType);
                /*
                 * 26-Feb-2011, tatu: To help with [JACKSON-518], we better recognize cases where
                 * type degenerates back into "Object.class" (as is the case with plain TypeVariable,
                 * for example), and not use that.
                 */
                if (rootType.getRawClass() == Object.class) {
                    rootType = null;
                }
            }
        }

        return rootType;
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

    public enum SerializationInclude {

        ALWAYS,
        NON_NULL,
        NON_ABSENT,
        NON_EMPTY;

        public static SerializationInclude decode(Object object, SerializerProvider serializerProvider) {
            JsonInclude.Include include = serializerProvider.getDefaultPropertyInclusion(object.getClass()).getValueInclusion();
            return switch (include) {
                case NON_EMPTY -> NON_EMPTY;
                case NON_NULL -> NON_NULL;
                case NON_ABSENT -> NON_ABSENT;
                default -> ALWAYS;
            };
        }

        public boolean shouldSerialize(Object value) {
            return switch (this) {
                case ALWAYS -> true;
                case NON_NULL -> value != null;
                case NON_ABSENT -> isPresent(value);
                case NON_EMPTY -> hasValue(value);
            };
        }

        private boolean isPresent(Object value) {
            if (value == null) {
                return false;
            }
            if (value instanceof Optional o) {
                return o.isPresent();
            }
            return true;
        }

        private boolean hasValue(Object value) {
            if (!isPresent(value)) {
                return false;
            }
            if (value instanceof String s) {
                return !s.isEmpty();
            }
            if (value instanceof Collection c) {
                return !c.isEmpty();
            }
            if (value instanceof Map m) {
                return !m.isEmpty();
            }
            if (value.getClass().isArray()) {
                return Array.getLength(value) > 0;
            }
            return true;
        }
    }
}
