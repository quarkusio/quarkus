package io.quarkus.security.runtime;

import static io.quarkus.security.spi.runtime.BlockingSecurityExecutor.createBlockingExecutor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;

/**
 * A manager that can be used to get a specific type of identity provider.
 */
public class QuarkusIdentityProviderManagerImpl implements IdentityProviderManager {
    private static final Logger log = Logger.getLogger(QuarkusIdentityProviderManagerImpl.class);

    private final Map<Class<? extends AuthenticationRequest>, List<IdentityProvider>> providers;
    private final SecurityIdentityAugmentor[] augmenters;
    private final AuthenticationRequestContext blockingRequestContext;

    QuarkusIdentityProviderManagerImpl(Builder builder) {
        this.providers = builder.providers;
        this.augmenters = builder.augmentors.toArray(SecurityIdentityAugmentor[]::new);
        this.blockingRequestContext = new AuthenticationRequestContext() {
            @Override
            public Uni<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
                return builder.blockingExecutor.executeBlocking(function);
            }
        };
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
    public Uni<SecurityIdentity> authenticate(AuthenticationRequest request) {
        try {
            List<IdentityProvider> providers = this.providers.get(request.getClass());
            if (providers == null) {
                return Uni.createFrom().failure(new IllegalArgumentException(
                        "No IdentityProviders were registered to handle AuthenticationRequest " + request));
            }
            if (providers.size() == 1) {
                return handleSingleProvider(providers.get(0), request);
            }
            return handleProvider(0, (List) providers, request);
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }

    private Uni<SecurityIdentity> handleSingleProvider(IdentityProvider identityProvider, AuthenticationRequest request) {
        Uni<SecurityIdentity> authenticated = identityProvider.authenticate(request, blockingRequestContext)
                .onItem().ifNull().failWith(new Supplier<Throwable>() {
                    @Override
                    public Throwable get() {
                        // reject request with the invalid credential
                        return new AuthenticationFailedException();
                    }
                });
        if (augmenters.length > 0) {
            authenticated = authenticated
                    .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<? extends SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                            return handleIdentityFromProvider(0, securityIdentity, request.getAttributes());
                        }
                    });
        }
        return authenticated;
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
            throw new IllegalArgumentException(
                    "No IdentityProviders were registered to handle AuthenticationRequest " + request);
        }
        return (SecurityIdentity) handleProvider(0, (List) providers, request).await().indefinitely();
    }

    private <T extends AuthenticationRequest> Uni<SecurityIdentity> handleProvider(int pos,
            List<IdentityProvider<T>> providers, T request) {
        if (pos == providers.size()) {
            //we failed to authentication
            log.debug("Authentication failed as providers would authenticate the request");
            return Uni.createFrom().failure(new AuthenticationFailedException());
        }
        IdentityProvider<T> current = providers.get(pos);
        Uni<SecurityIdentity> cs = current.authenticate(request, blockingRequestContext)
                .onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                        if (securityIdentity != null) {
                            return Uni.createFrom().item(securityIdentity);
                        }
                        return handleProvider(pos + 1, providers, request);
                    }
                });
        return cs.onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
            @Override
            public Uni<? extends SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                return handleIdentityFromProvider(0, securityIdentity, request.getAttributes());
            }
        });
    }

    private Uni<SecurityIdentity> handleIdentityFromProvider(int pos, SecurityIdentity identity,
            Map<String, Object> attributes) {
        if (pos == augmenters.length) {
            return Uni.createFrom().item(identity);
        }
        SecurityIdentityAugmentor a = augmenters[pos];
        return a.augment(identity, blockingRequestContext, attributes)
                .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                        return handleIdentityFromProvider(pos + 1, securityIdentity, attributes);
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
        private final List<SecurityIdentityAugmentor> augmentors = new ArrayList<>();
        private BlockingSecurityExecutor blockingExecutor;
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
        public Builder addSecurityIdentityAugmentor(SecurityIdentityAugmentor augmentor) {
            augmentors.add(augmentor);
            return this;
        }

        /**
         * @param blockingExecutor The executor to use for blocking tasks
         * @return this builder
         */
        public Builder setBlockingExecutor(BlockingSecurityExecutor blockingExecutor) {
            this.blockingExecutor = blockingExecutor;
            return this;
        }

        /**
         * @param blockingExecutor The executor to use for blocking tasks
         * @return this builder
         */
        public Builder setBlockingExecutor(Executor blockingExecutor) {
            this.blockingExecutor = createBlockingExecutor(() -> blockingExecutor);
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
            augmentors.sort(new Comparator<SecurityIdentityAugmentor>() {
                @Override
                public int compare(SecurityIdentityAugmentor o1, SecurityIdentityAugmentor o2) {
                    return Integer.compare(o2.priority(), o1.priority());
                }
            });
            return new QuarkusIdentityProviderManagerImpl(this);
        }
    }

}
