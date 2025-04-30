package io.quarkus.tls.runtime.config;

import java.util.Optional;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface KeyStoreCredentialProviderConfig {

    /**
     * The name of the "credential" bucket (map key -> passwords) to retrieve from the
     * {@link io.quarkus.credentials.CredentialsProvider}. If not set, the credential provider will not be used.
     * <p>
     * A credential provider offers a way to retrieve the key store password as well as alias password.
     * Note that the credential provider is only used if the passwords are not set in the configuration.
     */
    Optional<String> name();

    /**
     * The name of the bean providing the credential provider.
     * <p>
     * The name is used to select the credential provider to use.
     * The credential provider must be exposed as a CDI bean and with the {@code @Named} annotation set to the
     * configured name to be selected.
     * <p>
     * If not set, the default credential provider is used.
     */
    Optional<String> beanName();

    /**
     * The key used to retrieve the key store password.
     * <p>
     * If the selected credential provider does not support the key, the password is not retrieved.
     * Otherwise, the retrieved value is used to open the key store.
     */
    @WithDefault(CredentialsProvider.PASSWORD_PROPERTY_NAME)
    String passwordKey();

    /**
     * The key used to retrieve the key store alias password.
     * <p>
     * If the selected credential provider does not contain the key, the alias password is not retrieved.
     * Otherwise, the retrieved value is used to access the alias {@code private key} from the key store.
     */
    @WithDefault("alias-password")
    String aliasPasswordKey();
}
