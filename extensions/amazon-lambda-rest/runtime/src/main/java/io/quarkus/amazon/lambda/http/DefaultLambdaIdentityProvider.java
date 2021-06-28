package io.quarkus.amazon.lambda.http;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
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
        AwsProxyRequest event = request.getEvent();
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
    public static SecurityIdentity authenticate(AwsProxyRequest event) {
        Principal principal = getPrincipal(event);
        if (principal == null) {
            return null;
        }
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(principal);
        return builder.build();
    }

    public static Principal getPrincipal(AwsProxyRequest request) {
        final Map<String, String> systemEnvironment = System.getenv();
        final boolean isSamLocal = Boolean.parseBoolean(systemEnvironment.get("AWS_SAM_LOCAL"));
        final AwsProxyRequestContext requestContext = request.getRequestContext();
        if (isSamLocal && (requestContext == null
                || (requestContext.getAuthorizer() == null && requestContext.getIdentity() == null))) {
            final String forcedUserName = systemEnvironment.get("QUARKUS_AWS_LAMBDA_FORCE_USER_NAME");
            if (forcedUserName != null && !forcedUserName.isEmpty()) {
                return new QuarkusPrincipal(forcedUserName);
            }
        } else {
            if (requestContext != null) {
                if (requestContext.getIdentity() != null && requestContext.getIdentity().getUser() != null) {
                    return new IAMPrincipal(requestContext.getIdentity());
                } else if (requestContext.getAuthorizer() != null) {
                    if (requestContext.getAuthorizer().getClaims() != null) {
                        return new CognitoPrincipal(requestContext.getAuthorizer().getClaims());
                    } else if (requestContext.getAuthorizer().getPrincipalId() != null) {
                        return new CustomPrincipal(requestContext.getAuthorizer().getPrincipalId(),
                                requestContext.getAuthorizer().getContextProperties());
                    }
                }
            }
        }
        return null;
    }

}
