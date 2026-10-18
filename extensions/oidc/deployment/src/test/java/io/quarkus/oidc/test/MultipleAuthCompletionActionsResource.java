package io.quarkus.oidc.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/auth-completion-counts")
@Authenticated
public class MultipleAuthCompletionActionsResource {

    @Inject
    TestAuthenticationCompletionAction authCompletion;

    @Inject
    TestAuthenticationCompletionAction2 authCompletion2;

    @GET
    public String getAllAuthenticationCompletionCallCounts() {
        return authCompletion.getCallCounter() + ":" + authCompletion2.getCallCounter();
    }
}
