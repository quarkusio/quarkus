package io.quarkus.tls.runtime.keystores;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.inject.literal.NamedLiteral;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.tls.runtime.config.KeyStoreCredentialProviderConfig;
import io.quarkus.tls.runtime.config.TrustStoreCredentialProviderConfig;

public class CredentialProviders {

    public static Optional<String> getKeyStorePassword(Optional<String> maybePasswordFromConfig,
            KeyStoreCredentialProviderConfig config) {
        if (maybePasswordFromConfig.isPresent()) {
            return maybePasswordFromConfig;
        }
        if (config.name().isPresent()) {
            CredentialsProvider provider = lookup(config.beanName().orElse(null));
            Map<String, String> credentials = provider.getCredentials(config.name().get());
            return Optional.ofNullable(credentials.get(config.passwordKey()));
        }
        return Optional.empty();
    }

    public static Optional<String> getAliasPassword(Optional<String> maybePasswordFromConfig,
            KeyStoreCredentialProviderConfig config) {
        if (maybePasswordFromConfig.isPresent()) {
            return maybePasswordFromConfig;
        }
        if (config.name().isPresent()) {
            CredentialsProvider provider = lookup(config.beanName().orElse(null));
            Map<String, String> credentials = provider.getCredentials(config.name().get());
            return Optional.ofNullable(credentials.get(config.aliasPasswordKey()));
        }
        return Optional.empty();
    }

    public static Optional<String> getTrustStorePassword(Optional<String> maybePasswordFromConfig,
            TrustStoreCredentialProviderConfig config) {
        if (maybePasswordFromConfig.isPresent()) {
            return maybePasswordFromConfig;
        }
        if (config.name().isPresent()) {
            CredentialsProvider provider = lookup(config.beanName().orElse(null));
            Map<String, String> credentials = provider.getCredentials(config.name().get());
            return Optional.ofNullable(credentials.get(config.passwordKey()));
        }
        return Optional.empty();
    }

    static CredentialsProvider lookup(String name) {
        ArcContainer container = Arc.container();
        InstanceHandle<CredentialsProvider> instance;
        if (name == null) {
            instance = container.instance(CredentialsProvider.class);
        } else {
            instance = container.instance(CredentialsProvider.class, NamedLiteral.of(name));
        }

        if (!instance.isAvailable()) {
            if (name == null) {
                throw new RuntimeException("Unable to find the default credentials provider");
            } else {
                throw new RuntimeException("Unable to find the credentials provider named '" + name + "'");
            }
        }

        return instance.get();
    }
}
