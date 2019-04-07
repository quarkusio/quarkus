package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultAppRoleAuthenticationConfig {

    /**
     * Role Id for AppRole auth method. This property is required when selecting the app-role authentication type.
     */
    @ConfigItem
    public Optional<String> roleId;

    /**
     * Secret Id for AppRole auth method. This property is required when selecting the app-role authentication type.
     */
    @ConfigItem
    public Optional<String> secretId;

}
