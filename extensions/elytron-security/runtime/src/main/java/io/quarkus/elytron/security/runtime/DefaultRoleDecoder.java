package io.quarkus.elytron.security.runtime;

import java.util.HashSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

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
@ApplicationScoped
public class DefaultRoleDecoder implements RoleDecoder {

    private static final String DEFAULT_ATTRIBUTE_NAME = "groups";

    @Inject
    private Instance<RoleDecoder> instances;

    @Override
    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
        RoleDecoder delegate = getDelegate();

        if (delegate == null) {
            return fromDefaultAttribute(authorizationIdentity);
        }

        return delegate.decodeRoles(authorizationIdentity);
    }

    private Roles fromDefaultAttribute(AuthorizationIdentity authorizationIdentity) {
        Attributes.Entry groups = authorizationIdentity.getAttributes().get(DEFAULT_ATTRIBUTE_NAME);

        if (groups == null) {
            return Roles.NONE;
        }

        return Roles.fromSet(new HashSet<>(groups));
    }

    private RoleDecoder getDelegate() {
        return instances.stream().filter(roleDecoder -> !DefaultRoleDecoder.class.isInstance(roleDecoder)).findAny()
                .orElse(null);
    }
}
