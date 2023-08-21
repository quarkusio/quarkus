package io.quarkus.oidc.token.propagation.reactive;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC Token Propagation Reactive.
 */
@ConfigRoot
public class OidcTokenPropagationReactiveBuildTimeConfig {
    /**
     * If the OIDC Token Reactive Propagation is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

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
    @ConfigItem(defaultValue = "false")
    public boolean enabledDuringAuthentication;
}
