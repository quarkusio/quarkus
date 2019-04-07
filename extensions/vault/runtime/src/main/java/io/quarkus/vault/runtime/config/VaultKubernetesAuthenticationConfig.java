package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_KUBERNETES_JWT_TOKEN_PATH;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultKubernetesAuthenticationConfig {

    /**
     * Kubernetes authentication role that has been created in Vault to associate Vault policies, with
     * Kubernetes service accounts and/or Kubernetes namespaces. This property is required when selecting
     * the Kubernetes authentication type.
     */
    @ConfigItem
    public Optional<String> role;

    /**
     * Location of the file containing the Kubernetes JWT token to authenticate against
     * in Kubernetes authentication mode.
     */
    @ConfigItem(defaultValue = DEFAULT_KUBERNETES_JWT_TOKEN_PATH)
    public String jwtTokenPath;
}
