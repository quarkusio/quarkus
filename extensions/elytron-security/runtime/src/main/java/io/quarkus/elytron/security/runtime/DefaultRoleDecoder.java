package io.quarkus.elytron.security.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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
public class DefaultRoleDecoder {

    @ConfigProperty(name = "quarkus.security.roles-claim-name", defaultValue = "groups")
    String groupsAttribute;

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

    Roles fromDefaultAttribute(AuthorizationIdentity authorizationIdentity) {
        Attributes.Entry groups = authorizationIdentity.getAttributes().get(groupsAttribute);

        if (groups == null) {
            return Roles.NONE;
        }

        return Roles.fromSet(new HashSet<>(groups));
    }
}
