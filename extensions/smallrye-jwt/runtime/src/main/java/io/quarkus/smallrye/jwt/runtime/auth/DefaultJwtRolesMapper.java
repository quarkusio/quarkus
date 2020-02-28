package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.HashSet;

import javax.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;

import io.quarkus.arc.DefaultBean;

@DefaultBean
@ApplicationScoped
public class DefaultJwtRolesMapper implements JwtRolesMapper {

    @Override
    public HashSet<String> mapGroupsAndRoles(JwtClaims claims) throws MalformedClaimException {
        return new HashSet<>(claims.getStringListClaimValue("groups"));
    }
}
