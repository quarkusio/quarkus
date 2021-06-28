package io.quarkus.amazon.lambda.http;

import java.util.Optional;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Helper interface that removes some boilerplate for creating
 * an IdentityProvider that processes APIGatewayV2HTTPEvent
 */
public interface LambdaIdentityProvider extends IdentityProvider<LambdaAuthenticationRequest> {
    @Override
    default public Class<LambdaAuthenticationRequest> getRequestType() {
        return LambdaAuthenticationRequest.class;
    }

    @Override
    default Uni<SecurityIdentity> authenticate(LambdaAuthenticationRequest request, AuthenticationRequestContext context) {
        AwsProxyRequest event = request.getEvent();
        SecurityIdentity identity = authenticate(event);
        if (identity == null) {
            return Uni.createFrom().optional(Optional.empty());
        }
        return Uni.createFrom().item(identity);
    }

    /**
     * You must override this method unless you directly override
     * IdentityProvider.authenticate
     *
     * @param event
     * @return
     */
    default SecurityIdentity authenticate(AwsProxyRequest event) {
        throw new IllegalStateException("You must override this method or IdentityProvider.authenticate");
    }
}
