package io.quarkus.amazon.lambda.http;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.amazon.lambda.http.model.CognitoAuthorizerClaims;

/**
 * Allocated when cognito is used to authenticate user
 *
 * Will only be allocated if requestContext.authorizer.claims.cognito:username is set
 * in the http event sent by API Gateway
 *
 */
public class CognitoPrincipal implements JsonWebToken {
    private CognitoAuthorizerClaims claims;
    private String name;
    private Set<String> audience;
    private Set<String> groups;

    public CognitoPrincipal(CognitoAuthorizerClaims claims) {
        this.claims = claims;
        this.name = claims.getUsername();
    }

    @Override
    public Set<String> getClaimNames() {
        return null;
    }

    @Override
    public <T> T getClaim(String claimName) {
        if (claimName.equals(Claims.groups)) {
            return (T) getGroups();
        } else if (claimName.equals(Claims.groups)) {
            return (T) getAudience();
        } else if (claimName.equals(Claims.exp)) {
            return (T) Long.valueOf(getExpirationTime());
        } else if (claimName.equals(Claims.iat)) {
            return (T) Long.valueOf(getIssuedAtTime());
        } else if (claimName.equals(Claims.aud)) {
            return (T) getAudience();
        }
        return (T) claims.getClaim(claimName);
    }

    @Override
    public String getIssuer() {
        return claims.getIssuer();
    }

    @Override
    public Set<String> getAudience() {
        if (audience == null) {
            audience = new HashSet<>();
            audience.add(claims.getAudience());
        }
        return audience;
    }

    @Override
    public String getSubject() {
        return claims.getSubject();
    }

    @Override
    public long getExpirationTime() {
        return Long.valueOf(claims.getExpiration());
    }

    @Override
    public long getIssuedAtTime() {
        return Long.valueOf(claims.getIssuedAt());
    }

    @Override
    public Set<String> getGroups() {
        if (groups == null) {
            String grpClaim = claims.getClaim(LambdaHttpRecorder.config.cognitoRoleClaim());
            if (grpClaim != null) {
                Matcher matcher = LambdaHttpRecorder.groupPattern.matcher(grpClaim);
                groups = new HashSet<>();
                while (matcher.find()) {
                    groups.add(matcher.group(matcher.groupCount()));
                }

            } else {
                groups = Collections.EMPTY_SET;
            }
        }
        return groups;
    }

    @Override
    public String getName() {
        return name;
    }

    public CognitoAuthorizerClaims getClaims() {
        return claims;
    }
}
