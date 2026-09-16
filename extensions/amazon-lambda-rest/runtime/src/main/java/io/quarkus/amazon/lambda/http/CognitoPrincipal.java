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
        return claims.getClaimNames();
    }

    @Override
    public <T> T getClaim(String claimName) {
        if (Claims.groups.name().equals(claimName)) {
            return (T) getGroups();
        } else if (Claims.exp.name().equals(claimName)) {
            return (T) Long.valueOf(getExpirationTime());
        } else if (Claims.iat.name().equals(claimName)) {
            return (T) Long.valueOf(getIssuedAtTime());
        } else if (Claims.aud.name().equals(claimName)) {
            return (T) getAudience();
        } else if (Claims.sub.name().equals(claimName)) {
            return (T) getSubject();
        } else if (Claims.iss.name().equals(claimName)) {
            return (T) getIssuer();
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
        return parseTime(claims.getExpiration());
    }

    @Override
    public long getIssuedAtTime() {
        return parseTime(claims.getIssuedAt());
    }

    private static long parseTime(String value) {
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
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
