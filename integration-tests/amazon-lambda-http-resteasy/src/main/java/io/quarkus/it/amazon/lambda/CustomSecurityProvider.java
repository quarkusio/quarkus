package io.quarkus.it.amazon.lambda;

import java.security.Principal;

import javax.enterprise.context.ApplicationScoped;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.amazon.lambda.http.LambdaIdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

@ApplicationScoped
public class CustomSecurityProvider implements LambdaIdentityProvider {
    @Override
    public SecurityIdentity authenticate(APIGatewayV2HTTPEvent event) {
        if (event.getHeaders() == null || !event.getHeaders().containsKey("x-user"))
            return null;
        Principal principal = new QuarkusPrincipal(event.getHeaders().get("x-user"));
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(principal);
        return builder.build();
    }
}
