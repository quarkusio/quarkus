package io.quarkus.security.runtime;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;

/**
 * CDI bean than manages the lifecycle of the {@link io.quarkus.security.identity.IdentityProviderManager}
 */
@ApplicationScoped
public class IdentityProviderManagerCreator {

    @Inject
    Instance<IdentityProvider<?>> identityProviders;

    @Inject
    Instance<SecurityIdentityAugmentor> augmentors;

    @Inject
    BlockingSecurityExecutor blockingExecutor;

    @ApplicationScoped
    @DefaultBean
    @Produces
    BlockingSecurityExecutor defaultBlockingExecutor() {
        return BlockingSecurityExecutor.createBlockingExecutor(new Supplier<Executor>() {
            @Override
            public Executor get() {
                return ExecutorRecorder.getCurrent();
            }
        });
    }

    @Produces
    @ApplicationScoped
    public IdentityProviderManager ipm() {
        boolean customAnon = false;
        QuarkusIdentityProviderManagerImpl.Builder builder = QuarkusIdentityProviderManagerImpl.builder();
        for (var i : identityProviders) {
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
        builder.setBlockingExecutor(blockingExecutor);
        return builder.build();
    }

}
