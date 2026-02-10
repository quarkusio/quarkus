package io.quarkus.oidc.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/auth-completion-count")
@Authenticated
public class AuthenticationCompletionActionResource {

    @Inject
    TestAuthenticationCompletionAction authCompletion;

    @GET
    public String getAuthenticationCompletionCallCount() {
        return Integer.toString(authCompletion.getCallCounter());
    }
}
