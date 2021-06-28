package io.quarkus.amazon.lambda.http;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
final public class DefaultLambdaIdentityProvider implements IdentityProvider<DefaultLambdaAuthenticationRequest> {

    @Override
    public Class<DefaultLambdaAuthenticationRequest> getRequestType() {
        return DefaultLambdaAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(DefaultLambdaAuthenticationRequest request,
            AuthenticationRequestContext context) {
        APIGatewayV2HTTPEvent event = request.getEvent();
        SecurityIdentity identity = authenticate(event);
        if (identity == null) {
            return Uni.createFrom().optional(Optional.empty());
        }
        return Uni.createFrom().item(identity);
    }

    /**
     * Create a SecurityIdentity with a principal derived from APIGatewayV2HTTPEvent.
     * Looks for Cognito JWT, IAM, or Custom Lambda metadata for principal name
     *
     * @param event
     * @return
     */
    public static SecurityIdentity authenticate(APIGatewayV2HTTPEvent event) {
        Principal principal = getPrincipal(event);
        if (principal == null) {
            return null;
        }
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(principal);
        return builder.build();
    }

    protected static Principal getPrincipal(APIGatewayV2HTTPEvent request) {
        final Map<String, String> systemEnvironment = System.getenv();
        final boolean isSamLocal = Boolean.parseBoolean(systemEnvironment.get("AWS_SAM_LOCAL"));
        final APIGatewayV2HTTPEvent.RequestContext requestContext = request.getRequestContext();
        if (isSamLocal && (requestContext == null || requestContext.getAuthorizer() == null)) {
            final String forcedUserName = systemEnvironment.get("QUARKUS_AWS_LAMBDA_FORCE_USER_NAME");
            if (forcedUserName != null && !forcedUserName.isEmpty()) {
                return new QuarkusPrincipal(forcedUserName);
            }
        } else {
            if (requestContext != null) {
                final APIGatewayV2HTTPEvent.RequestContext.Authorizer authorizer = requestContext.getAuthorizer();
                if (authorizer != null) {
                    if (authorizer.getJwt() != null) {
                        final APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt = authorizer.getJwt();
                        final Map<String, String> claims = jwt.getClaims();
                        if (claims != null && claims.containsKey("cognito:username")) {
                            return new CognitoPrincipal(jwt);
                        }
                    } else if (authorizer.getIam() != null) {
                        if (authorizer.getIam().getUserId() != null) {
                            return new IAMPrincipal(authorizer.getIam());
                        }
                    } else if (authorizer.getLambda() != null) {
                        Object tmp = authorizer.getLambda().get("principalId");
                        if (tmp != null && tmp instanceof String) {
                            String username = (String) tmp;
                            return new CustomPrincipal(username, authorizer.getLambda());
                        }
                    }
                }
            }
        }
        return null;
    }

}
