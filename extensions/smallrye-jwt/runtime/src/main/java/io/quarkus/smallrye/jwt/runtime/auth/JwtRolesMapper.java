package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.HashSet;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;

public interface JwtRolesMapper {
    HashSet<String> mapGroupsAndRoles(JwtClaims claims) throws MalformedClaimException;
}
