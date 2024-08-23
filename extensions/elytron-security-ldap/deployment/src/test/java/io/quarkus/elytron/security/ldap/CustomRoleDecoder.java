package io.quarkus.elytron.security.ldap;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;

import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;

@ApplicationScoped
public class CustomRoleDecoder implements RoleDecoder {

    @Override
    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
        Attributes.Entry groupsEntry = authorizationIdentity.getAttributes().get("Roles");
        Set<String> roles = new HashSet<>();
        StreamSupport.stream(groupsEntry.spliterator(), false).forEach(groups -> {
            for (String role : groups.split(",")) {
                roles.add(role.trim());
            }
        });
        return Roles.fromSet(roles);
    }
}
