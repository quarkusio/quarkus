package io.quarkus.security.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.AuthenticationRequest;

/**
 * A manager that can be used to get a specific type of identity provider.
 */
public class QuarkusIdentityProviderManagerImpl implements IdentityProviderManager {
    private static final Logger log = Logger.getLogger(QuarkusIdentityProviderManagerImpl.class);

    private final Map<Class<? extends AuthenticationRequest>, List<IdentityProvider>> providers;
    private final List<SecurityIdentityAugmentor> augmenters;
    private final Executor blockingExecutor;

    private static final AuthenticationRequestContext blockingRequestContext = new AuthenticationRequestContext() {
        @Override
        public CompletionStage<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
            CompletableFuture<SecurityIdentity> ret = new CompletableFuture<>();
            try {
                SecurityIdentity result = function.get();
                ret.complete(result);
            } catch (Throwable t) {
                ret.completeExceptionally(t);
            }
            return ret;
        }
    };

    QuarkusIdentityProviderManagerImpl(Builder builder) {
        this.providers = builder.providers;
        this.augmenters = builder.augmenters;
        this.blockingExecutor = builder.blockingExecutor;
    }

    /**
     * Attempts to create an authenticated identity for the provided {@link AuthenticationRequest}.
     * <p>
     * If authentication succeeds the resulting identity will be augmented with any configured {@link SecurityIdentityAugmentor}
     * instances that have been registered.
     *
     * @param request The authentication request
     * @return The first identity provider that was registered with this type
     */
    public CompletionStage<SecurityIdentity> authenticate(AuthenticationRequest request) {
        List<IdentityProvider> providers = this.providers.get(request.getClass());
        if (providers == null) {
            CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
            cf.completeExceptionally(new IllegalArgumentException(
                    "No IdentityProviders were registered to handle AuthenticationRequest " + request));
            return cf;
        }
        return handleProvider(0, (List) providers, request, new AsyncAthenticationRequestContext());
    }

    /**
     * Attempts to create an authenticated identity for the provided {@link AuthenticationRequest} in a blocking manner
     * <p>
     * If authentication succeeds the resulting identity will be augmented with any configured {@link SecurityIdentityAugmentor}
     * instances that have been registered.
     *
     * @param request The authentication request
     * @return The first identity provider that was registered with this type
     */
    public SecurityIdentity authenticateBlocking(AuthenticationRequest request) {
        List<IdentityProvider> providers = this.providers.get(request.getClass());
        if (providers == null) {
            CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
            throw new IllegalArgumentException(
                    "No IdentityProviders were registered to handle AuthenticationRequest " + request);
        }
        return (SecurityIdentity) handleProvider(0, (List) providers, request, blockingRequestContext).toCompletableFuture()
                .join();
    }

    private <T extends AuthenticationRequest> CompletionStage<SecurityIdentity> handleProvider(int pos,
            List<IdentityProvider<T>> providers, T request, AuthenticationRequestContext context) {
        if (pos == providers.size()) {
            //we failed to authentication
            log.debugf("Authentication failed as providers would authenticate the request");
            CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
            cf.completeExceptionally(new AuthenticationFailedException());
            return cf;
        }
        IdentityProvider<T> current = providers.get(pos);
        CompletionStage<SecurityIdentity> cs = current.authenticate(request, context)
                .thenCompose(new Function<SecurityIdentity, CompletionStage<SecurityIdentity>>() {
                    @Override
                    public CompletionStage<SecurityIdentity> apply(SecurityIdentity identity) {
                        if (identity != null) {
                            return CompletableFuture.completedFuture(identity);
                        }
                        return handleProvider(pos + 1, providers, request, context);
                    }
                });
        return cs.thenCompose(new Function<SecurityIdentity, CompletionStage<SecurityIdentity>>() {
            @Override
            public CompletionStage<SecurityIdentity> apply(SecurityIdentity identity) {
                return handleIdentityFromProvider(0, identity, context);
            }
        });
    }

    private CompletionStage<SecurityIdentity> handleIdentityFromProvider(int pos, SecurityIdentity identity,
            AuthenticationRequestContext context) {
        if (pos == augmenters.size()) {
            return CompletableFuture.completedFuture(identity);
        }
        SecurityIdentityAugmentor a = augmenters.get(pos);
        return a.augment(identity, context).thenCompose(new Function<SecurityIdentity, CompletionStage<SecurityIdentity>>() {
            @Override
            public CompletionStage<SecurityIdentity> apply(SecurityIdentity identity) {
                return handleIdentityFromProvider(pos + 1, identity, context);
            }
        });
    }

    /**
     * Creates a builder for constructing instances of {@link QuarkusIdentityProviderManagerImpl}
     *
     * @return A builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for constructing instances of {@link QuarkusIdentityProviderManagerImpl}
     */
    public static class Builder {

        Builder() {
        }

        private final Map<Class<? extends AuthenticationRequest>, List<IdentityProvider>> providers = new HashMap<>();
        private final List<SecurityIdentityAugmentor> augmenters = new ArrayList<>();
        private Executor blockingExecutor;
        private boolean built = false;

        /**
         * Adds an {@link IdentityProvider} implementation to this manager
         *
         * @param provider The provider
         * @return this builder
         */
        public Builder addProvider(IdentityProvider provider) {
            if (built) {
                throw new IllegalStateException("manager has already been built");
            }
            providers.computeIfAbsent(provider.getRequestType(), (a) -> new ArrayList<>()).add(provider);
            return this;
        }

        /**
         * Adds an augmentor that can modify the security identity that is provided by the identity store.
         *
         * @param augmentor The augmentor
         * @return this builder
         */
        public Builder addSecurityIdentityAugmenter(SecurityIdentityAugmentor augmentor) {
            augmenters.add(augmentor);
            return this;
        }

        /**
         * @param blockingExecutor The executor to use for blocking tasks
         * @return this builder
         */
        public Builder setBlockingExecutor(Executor blockingExecutor) {
            this.blockingExecutor = blockingExecutor;
            return this;
        }

        /**
         * @return a new {@link QuarkusIdentityProviderManagerImpl}
         */
        public QuarkusIdentityProviderManagerImpl build() {
            built = true;
            if (!providers.containsKey(AnonymousAuthenticationRequest.class)) {
                throw new IllegalStateException(
                        "No AnonymousIdentityProvider registered. An instance of AnonymousIdentityProvider must be provided to allow the Anonymous identity to be created.");
            }
            if (blockingExecutor == null) {
                throw new IllegalStateException("no blocking executor specified");
            }
            augmenters.sort(new Comparator<SecurityIdentityAugmentor>() {
                @Override
                public int compare(SecurityIdentityAugmentor o1, SecurityIdentityAugmentor o2) {
                    return Integer.compare(o2.priority(), o1.priority());
                }
            });
            return new QuarkusIdentityProviderManagerImpl(this);
        }
    }

    private class AsyncAthenticationRequestContext implements AuthenticationRequestContext {

        private boolean inBlocking = false;

        @Override
        public CompletionStage<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
            if (inBlocking) {
                return blockingRequestContext.runBlocking(function);
            }
            CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
            blockingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        inBlocking = true;
                        cf.complete(function.get());
                    } catch (Throwable t) {
                        cf.completeExceptionally(t);
                    } finally {
                        inBlocking = false;
                    }
                }
            });

            return cf;
        }
    }
}
