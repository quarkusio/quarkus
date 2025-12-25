package io.quarkus.security.spi.runtime;

import java.util.Collection;
import java.util.List;

import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.common.annotation.Experimental;

/**
 * Builder API which allows extensions like the Quarkus Security JPA to create {@link IdentityProvider}s
 * programmatically. This builder is used together with the CDI event 'HttpSecurity'.
 */
@Experimental("This API is currently experimental and might get changed")
public interface IdentityProviderBuilder {

    /**
     * @return {@link IdentityProvider}s or null
     */
    Collection<IdentityProvider<?>> identityProviders();

    /**
     * @return {@link SecurityIdentityAugmentor}s or null; if the return value is not null or empty, only
     *         augmentors specified in the returned collection will be applied and global augmentors registered
     *         via CDI will be ignored
     */
    Collection<SecurityIdentityAugmentor> securityIdentityAugmentors();

    /**
     * @param augmentors {@link SecurityIdentityAugmentor}s; must not be null
     * @return {@link IdentityProviderBuilder}
     */
    static IdentityProviderBuilder of(SecurityIdentityAugmentor... augmentors) {
        record IdentityProviderBuilderImpl(Collection<IdentityProvider<?>> identityProviders,
                Collection<SecurityIdentityAugmentor> securityIdentityAugmentors)
                implements
                    IdentityProviderBuilder {
        }
        return new IdentityProviderBuilderImpl(null, List.of(augmentors));
    }
}
