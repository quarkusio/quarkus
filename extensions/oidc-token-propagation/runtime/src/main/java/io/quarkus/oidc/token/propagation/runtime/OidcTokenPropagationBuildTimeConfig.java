package io.quarkus.oidc.token.propagation.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for OIDC Token Propagation.
 */
@ConfigMapping(prefix = "quarkus.resteasy-client-oidc-token-propagation")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface OidcTokenPropagationBuildTimeConfig {

    /**
     * If the OIDC Token Propagation is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether the token propagation is enabled during the `SecurityIdentity` augmentation.
     *
     * For example, you may need to use a REST client from `SecurityIdentityAugmentor`
     * to propagate the current token to acquire additional roles for the `SecurityIdentity`.
     *
     * Note, this feature relies on a duplicated context. More information about Vert.x duplicated
     * context can be found in xref:duplicated-context.adoc[this guide].
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean enabledDuringAuthentication();
}
