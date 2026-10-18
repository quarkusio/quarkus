package io.quarkus.test.security.jwt;

import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.JsonValue;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.TestSecurityIdentityAugmentor;

public class JwtTestSecurityIdentityAugmentor implements TestSecurityIdentityAugmentor {
    private static Map<String, ClaimType> standardClaimTypes = Map.of(
            Claims.exp.name(), ClaimType.LONG,
            Claims.iat.name(), ClaimType.LONG,
            Claims.nbf.name(), ClaimType.LONG,
            Claims.auth_time.name(), ClaimType.LONG,
            Claims.email_verified.name(), ClaimType.BOOLEAN);

    @Override
    public SecurityIdentity augment(final SecurityIdentity identity, final Annotation[] annotations) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        final JwtSecurity jwtSecurity = findJwtSecurity(annotations);
        builder.setPrincipal(new JsonWebToken() {

            @Override
            public String getName() {
                return identity.getPrincipal().getName();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getClaim(String claimName) {
                if (Claims.groups.name().equals(claimName)) {
                    return (T) identity.getRoles();
                }
                if (jwtSecurity != null && jwtSecurity.claims() != null) {
                    for (Claim claim : jwtSecurity.claims()) {
                        if (claim.key().equals(claimName)) {
                            return (T) wrapValue(claim, convertClaimValue(claim));
                        }
                    }
                }
                return null;
            }

            @Override
            public Set<String> getClaimNames() {
                if (jwtSecurity != null && jwtSecurity.claims() != null) {
                    return Arrays.stream(jwtSecurity.claims()).map(Claim::key).collect(Collectors.toSet());
                }
                return Collections.emptySet();
            }

        });

        return builder.build();
    }

    private static JwtSecurity findJwtSecurity(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            if (ann instanceof JwtSecurity) {
                return (JwtSecurity) ann;
            }
        }
        return null;
    }

    private Object wrapValue(Claim claim, Object convertedClaimValue) {
        Claims claimType = getClaimType(claim.key());
        if (Claims.UNKNOWN == claimType) {
            if (convertedClaimValue instanceof Long) {
                return jsonProvider().createValue((Long) convertedClaimValue);
            } else if (convertedClaimValue instanceof Integer) {
                return jsonProvider().createValue((Integer) convertedClaimValue);
            } else if (convertedClaimValue instanceof Boolean) {
                return (Boolean) convertedClaimValue ? JsonValue.TRUE : JsonValue.FALSE;
            }
        }
        return convertedClaimValue;
    }

    protected Claims getClaimType(String claimName) {
        Claims claimType;
        try {
            claimType = Claims.valueOf(claimName);
        } catch (IllegalArgumentException e) {
            claimType = Claims.UNKNOWN;
        }
        return claimType;
    }

    private Object convertClaimValue(Claim claim) {
        ClaimType type = claim.type();
        if (type == ClaimType.DEFAULT && standardClaimTypes.containsKey(claim.key())) {
            type = standardClaimTypes.get(claim.key());
        }
        return type.convert(claim.value());
    }

}
