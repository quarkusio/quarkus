package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

public interface JwtRolesMapper {
    Set<String> mapRoles(JsonWebToken token);
}
