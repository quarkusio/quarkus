package io.quarkus.security.runtime;

import java.util.concurrent.Executor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;

/**
 * CDI bean than manages the lifecycle of the {@link io.quarkus.security.identity.IdentityProviderManager}
 */
@ApplicationScoped
public class IdentityProviderManagerCreator {

    @Inject
    Instance<IdentityProvider<?>> identityProviders;

    @Inject
    Instance<SecurityIdentityAugmentor> augmentors;

    @Produces
    @ApplicationScoped
    public IdentityProviderManager ipm() {
        boolean customAnon = false;
        QuarkusIdentityProviderManagerImpl.Builder builder = QuarkusIdentityProviderManagerImpl.builder();
        for (IdentityProvider i : identityProviders) {
            builder.addProvider(i);
            if (i.getRequestType() == AnonymousAuthenticationRequest.class) {
                customAnon = true;
            }
        }
        if (!customAnon) {
            builder.addProvider(new AnonymousIdentityProvider());
        }
        for (SecurityIdentityAugmentor i : augmentors) {
            builder.addSecurityIdentityAugmentor(i);
        }
        builder.setBlockingExecutor(new Executor() {
            @Override
            public void execute(Runnable command) {
                //TODO: should we be using vert.x blocking tasks here? We really should only have a single thread pool
                ExecutorRecorder.getCurrent().execute(command);
            }
        });
        return builder.build();
    }

}
