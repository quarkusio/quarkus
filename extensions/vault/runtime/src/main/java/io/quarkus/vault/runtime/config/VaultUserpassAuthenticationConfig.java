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
     * Password for userpass auth method. This property is required when selecting the userpass authentication type and not setting the password as wrapped.
     *
     * {@link VaultUserpassAuthenticationConfig#passwordWrapped}
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Wrapped token containing the password for userpass auth method. This flag is not mandatory, only required if password is wrapped.
     */
    @ConfigItem
    public Optional<String> passwordWrapped;

}
