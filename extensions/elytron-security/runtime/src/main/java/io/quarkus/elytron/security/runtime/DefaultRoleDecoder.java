package io.quarkus.elytron.security.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.enterprise.inject.Any;
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
public class DefaultRoleDecoder {

    private static final String DEFAULT_ATTRIBUTE_NAME = "groups";

    @Inject
    @Any
    private Instance<RoleDecoder> userInstances;

    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
        if (userInstances.isUnsatisfied()) {
            return fromDefaultAttribute(authorizationIdentity);
        } else if (userInstances.isAmbiguous()) {
            //if there are multiple decoders we return all the roles
            List<Roles> allRoles = new ArrayList<>();
            for (RoleDecoder i : userInstances) {
                allRoles.add(i.decodeRoles(authorizationIdentity));
            }
            Roles r = allRoles.get(0);
            for (int i = 1; i < allRoles.size(); ++i) {
                r = r.or(allRoles.get(i));
            }
            return r;
        } else {
            return userInstances.get().decodeRoles(authorizationIdentity);
        }
    }

    private Roles fromDefaultAttribute(AuthorizationIdentity authorizationIdentity) {
        Attributes.Entry groups = authorizationIdentity.getAttributes().get(DEFAULT_ATTRIBUTE_NAME);

        if (groups == null) {
            return Roles.NONE;
        }

        return Roles.fromSet(new HashSet<>(groups));
    }
}
