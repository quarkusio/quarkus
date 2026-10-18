package io.quarkus.oidc.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.AuthenticationCompletionAction;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Unremovable
public class TestAuthenticationCompletionAction implements AuthenticationCompletionAction {

    private volatile int callCounter;

    @Override
    public Uni<Void> action(AuthenticationCompletionContext authCompletionContext) {
        callCounter++;
        return Uni.createFrom().voidItem();
    }

    public int getCallCounter() {
        return callCounter;
    }

}
