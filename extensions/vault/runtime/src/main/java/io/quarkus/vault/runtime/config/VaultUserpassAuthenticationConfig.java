package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultUserpassAuthenticationConfig {

    /**
     * User for userpass auth method. This property is required when selecting the userpass authentication type.
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * Password for userpass auth method. This property is required when selecting the userpass authentication type.
     */
    @ConfigItem
    public Optional<String> password;

}
