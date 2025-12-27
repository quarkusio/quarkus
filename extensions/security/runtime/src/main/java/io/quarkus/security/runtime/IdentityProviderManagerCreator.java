package io.quarkus.security.runtime;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.security.spi.runtime.IdentityProviderManagerBuilder;

/**
 * CDI bean than manages the lifecycle of the {@link io.quarkus.security.identity.IdentityProviderManager}
 */
public class IdentityProviderManagerCreator {

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
    IdentityProviderManager ipm(Instance<IdentityProvider<?>> identityProviders,
            Instance<SecurityIdentityAugmentor> augmentors, BlockingSecurityExecutor blockingExecutor) {
        return createIdentityProviderManager(identityProviders, augmentors, blockingExecutor, null);
    }

    @Produces
    @ApplicationScoped
    IdentityProviderManagerBuilder identityProviderManagerBuilder(Instance<IdentityProvider<?>> globalIdentityProviders,
            Instance<SecurityIdentityAugmentor> globalAugmentors,
            BlockingSecurityExecutor blockingExecutor) {
        return (localIdentityProviders, localIdentityAugmentors) -> {
            final Iterable<IdentityProvider<?>> identityProviders;
            final Iterable<SecurityIdentityAugmentor> augmentors;
            final Iterable<SecurityIdentityAugmentor> additionalAugmentors;
            if (localIdentityProviders == null || localIdentityProviders.isEmpty()) {
                identityProviders = globalIdentityProviders;
            } else {
                identityProviders = localIdentityProviders;
            }
            if (localIdentityAugmentors == null || localIdentityAugmentors.isEmpty()) {
                augmentors = globalAugmentors;
                additionalAugmentors = null;
            } else {
                augmentors = localIdentityAugmentors;
                additionalAugmentors = keepOnlyBuiltinAugmentors(globalAugmentors);
            }
            return createIdentityProviderManager(identityProviders, augmentors, blockingExecutor, additionalAugmentors);
        };
    }

    private static QuarkusIdentityProviderManagerImpl createIdentityProviderManager(
            Iterable<IdentityProvider<?>> identityProviders, Iterable<SecurityIdentityAugmentor> augmentors,
            BlockingSecurityExecutor blockingExecutor, Iterable<SecurityIdentityAugmentor> additionalAugmentors) {
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
        if (additionalAugmentors != null) {
            for (SecurityIdentityAugmentor additionalAugmentor : additionalAugmentors) {
                builder.addSecurityIdentityAugmentor(additionalAugmentor);
            }
        }
        builder.setBlockingExecutor(blockingExecutor);
        return builder.build();
    }

    private static Iterable<SecurityIdentityAugmentor> keepOnlyBuiltinAugmentors(
            Iterable<SecurityIdentityAugmentor> augmentors) {
        for (SecurityIdentityAugmentor augmentor : augmentors) {
            if (augmentor instanceof QuarkusPermissionSecurityIdentityAugmentor) {
                // allows @PermissionCheckers grant permissions to identity
                return List.of(augmentor);
            }
        }
        return null;
    }
}
