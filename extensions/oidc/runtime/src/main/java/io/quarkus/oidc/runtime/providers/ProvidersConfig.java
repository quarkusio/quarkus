package io.quarkus.oidc.runtime.providers;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ProvidersConfig {

    /**
     * Config for using login with GitHub
     */
    @ConfigItem
    Optional<GitHub> github;

    /**
     * Config for using login with Facebook
     */
    @ConfigItem
    Optional<Facebook> facebook;

    /**
     * Config for using login with Google
     */
    @ConfigItem
    Optional<Google> google;

    /**
     * Config for using login with Microsoft
     */
    @ConfigItem
    Optional<Microsoft> microsoft;
}
