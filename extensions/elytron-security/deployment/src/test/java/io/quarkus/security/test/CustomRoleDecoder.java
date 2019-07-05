package io.quarkus.security.test;

import javax.enterprise.context.ApplicationScoped;

import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;

@ApplicationScoped
public class CustomRoleDecoder implements RoleDecoder {

    @Override
    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
        return Roles.of("custom");
    }
}
