package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.arc.DefaultBean;

@DefaultBean
@ApplicationScoped
public class DefaultJwtRolesMapper implements JwtRolesMapper {

    @Override
    public Set<String> mapRoles(JsonWebToken token) {
        return token.getGroups();
    }
}
