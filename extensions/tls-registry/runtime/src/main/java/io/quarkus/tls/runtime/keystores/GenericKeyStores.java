package io.quarkus.tls.runtime.keystores;

import static io.quarkus.tls.runtime.config.TlsConfigUtils.read;

import java.io.UncheckedIOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.NoSuchElementException;
import java.util.Optional;

import io.quarkus.tls.runtime.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.runtime.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.config.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptions;

public class GenericKeyStores {
    private GenericKeyStores() {
        // Avoid direct instantiation
    }

    public static KeyStoreAndKeyCertOptions verifyGenericKeyStore(KeyStoreConfig config, Vertx vertx, String name,
            String type) {
        var keyStoreConfig = config.generic().get(type);
        if (keyStoreConfig == null) {
            throw new NoSuchElementException("No such key store type " + type);
        }
        var options = toOptions(keyStoreConfig, config.credentialsProvider(), name, type);
        var keyStore = loadKeyStore(vertx, name, options, type, "key");
        verifyKeyStoreAlias(options, name, type, keyStore);
        return new KeyStoreAndKeyCertOptions(keyStore, options);
    }

    public static TrustStoreAndTrustOptions verifyGenericTrustStore(TrustStoreConfig config, Vertx vertx, String name,
            String type) {
        var trustStoreConfig = config.generic().get(type);
        if (trustStoreConfig == null) {
            throw new NoSuchElementException("No such trust store type " + type);
        }
        KeyStoreOptions options = toOptions(trustStoreConfig, config.credentialsProvider(), name, type);
        KeyStore ks = loadKeyStore(vertx, name, options, type, "trust");
        verifyTrustStoreAlias(trustStoreConfig.alias(), name, type, ks);
        if (config.certificateExpirationPolicy() == TrustStoreConfig.CertificateExpiryPolicy.IGNORE) {
            return new TrustStoreAndTrustOptions(ks, options);
        } else {
            var wrapped = new ExpiryTrustOptions(options, config.certificateExpirationPolicy());
            return new TrustStoreAndTrustOptions(ks, wrapped);
        }
    }

    private static KeyStoreOptions toOptions(GenericKeyStoreConfig config,
            KeyStoreCredentialProviderConfig keyStoreCredentialProviderConfig, String name, String type) {
        KeyStoreOptions options = new KeyStoreOptions();
        try {
            options.setType(type);
            options.setValue(Buffer.buffer(read(config.path())));
            String p = CredentialProviders.getKeyStorePassword(config.password(), keyStoreCredentialProviderConfig)
                    .orElse(null);
            if (p == null) {
                throw new IllegalArgumentException("Invalid " + type + " key store configuration for certificate '" + name
                        + "' - the key store password is not set and cannot be retrieved from the credential provider.");
            }
            options.setPassword(p);
            config.alias().ifPresent(options::setAlias);
            config.provider().ifPresent(options::setProvider);
            String ap = CredentialProviders.getAliasPassword(config.aliasPassword(), keyStoreCredentialProviderConfig)
                    .orElse(null);
            options.setAliasPassword(ap);
            return options;
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid " + type + " key store configuration for certificate '" + name
                    + "' - cannot read the key store file '" + config.path() + "'", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid " + type + " key store configuration for certificate '" + name + "'", e);
        }
    }

    private static KeyStoreOptions toOptions(GenericTrustStoreConfig config, TrustStoreCredentialProviderConfig cp, String name,
            String type) {
        KeyStoreOptions options = new KeyStoreOptions();
        try {
            options.setType(type);
            options.setValue(Buffer.buffer(read(config.path())));
            String password = CredentialProviders.getTrustStorePassword(config.password(), cp)
                    .orElse(null);
            if (password == null) {
                throw new IllegalStateException("Invalid " + type + " trust store configuration for certificate '" + name
                        + "' - the trust store password is not set and cannot be retrieved from the credential provider.");
            }
            options.setPassword(password);
            config.alias().ifPresent(options::setAlias);
            config.provider().ifPresent(options::setProvider);
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid " + type + " trust store configuration for certificate '" + name
                    + "' - cannot read the trust store file '" + config.path() + "'", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid " + type + " trust store configuration for certificate '" + name + "'", e);
        }
        return options;
    }

    private static void verifyKeyStoreAlias(KeyStoreOptions options, String name, String type, KeyStore ks) {
        String alias = options.getAlias();
        // Credential provider already called.
        String aliasPassword = options.getAliasPassword();
        if (alias != null) {
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in " + type + " key store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException(
                        "Unable to verify alias '" + alias + "' in " + type + " key store '" + name + "'", e);
            }

            char[] ap = null;
            if (aliasPassword != null) {
                ap = aliasPassword.toCharArray();
            }

            try {
                if (ks.getKey(alias, ap) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in " + type + " key store (private key not found)'" + name + "'");
                }
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in " + type + " key store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException | NoSuchAlgorithmException e) {
                throw new IllegalStateException(
                        "Unable to verify alias '" + alias + "' in " + type + " key store '" + name + "'", e);
            } catch (UnrecoverableKeyException e) {
                throw new IllegalArgumentException(
                        "Unable to recover the key for alias '" + alias + "' in " + type + " key store '" + name + "'", e);
            }
        }
    }

    private static void verifyTrustStoreAlias(Optional<String> maybeAlias, String name, String type, KeyStore ks) {
        if (maybeAlias.isPresent()) {
            String alias = maybeAlias.get();
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in " + type + " trust store (certificate not found)'" + name
                                    + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException(
                        "Unable to verify alias '" + alias + "' in " + type + " trust store '" + name + "'", e);
            }
        }
    }

    private static KeyStore loadKeyStore(Vertx vertx, String name, KeyStoreOptions options, String type,
            String keyOrTrustType) {
        try {
            return options.loadKeyStore(vertx);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to load  " + type + " " + keyOrTrustType + " store '" + name + "', verify the password.", e);
        }
    }
}
