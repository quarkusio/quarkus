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
     * Secret Id for AppRole auth method. This property is required when selecting the app-role authentication type and not setting the secret id as wrapped.
     *
     * {@link VaultAppRoleAuthenticationConfig#secretIdWrapped}
     */
    @ConfigItem
    public Optional<String> secretId;

    /**
     * Wrapped token containing the secret Id for AppRole auth method. This flag is not mandatory, only required if secret id is wrapped.
     */
    @ConfigItem
    public Optional<String> secretIdWrapped;

}
