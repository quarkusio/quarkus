package io.quarkus.elytron.security.runtime;

import java.util.HashSet;

import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;

/**
 * <p>
 * A default implementation of {@link RoleDecoder} that delegates decoding of roles for a given {@link AuthorizationIdentity} to
 * an application specific implementation of {@link RoleDecoder}, if provided.
 * 
 */
public class DefaultRoleDecoder implements RoleDecoder {

    private static final String DEFAULT_ATTRIBUTE_NAME = "groups";

    @Override
    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
        return fromDefaultAttribute(authorizationIdentity);
    }

    private Roles fromDefaultAttribute(AuthorizationIdentity authorizationIdentity) {
        Attributes.Entry groups = authorizationIdentity.getAttributes().get(DEFAULT_ATTRIBUTE_NAME);

        if (groups == null) {
            return Roles.NONE;
        }

        return Roles.fromSet(new HashSet<>(groups));
    }
}
