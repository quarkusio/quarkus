package io.quarkus.amazon.lambda.http;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

/**
 * Represents a Cognito JWT used to authenticate request
 *
 * Will only be allocated if requestContext.authorizer.jwt.claims.cognito:username is set
 * in the http event sent by API Gateway
 */
public class CognitoPrincipal implements JsonWebToken {
    private APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt;
    private String name;
    private Set<String> groups;

    public CognitoPrincipal(APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt) {
        this.jwt = jwt;
        this.name = jwt.getClaims().get("cognito:username");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getClaimNames() {
        return getClaims().getClaims().keySet();
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
        return (T) getClaims().getClaims().get(claimName);
    }

    @Override
    public Set<String> getAudience() {
        return Collections.EMPTY_SET;
    }

    @Override
    public long getExpirationTime() {
        String val = jwt.getClaims().get(Claims.exp);
        if (val == null)
            return 0;
        return Long.parseLong(val);
    }

    @Override
    public long getIssuedAtTime() {
        String val = jwt.getClaims().get(Claims.iat);
        if (val == null)
            return 0;
        return Long.parseLong(val);
    }

    @Override
    public Set<String> getGroups() {
        if (groups == null) {
            if (jwt.getClaims().containsKey(LambdaHttpRecorder.config.cognitoRoleClaim())) {
                String claim = jwt.getClaims().get(LambdaHttpRecorder.config.cognitoRoleClaim());
                Matcher matcher = LambdaHttpRecorder.groupPattern.matcher(claim);
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

    public APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT getClaims() {
        return jwt;
    }
}
