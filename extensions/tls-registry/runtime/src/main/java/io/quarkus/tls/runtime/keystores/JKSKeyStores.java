package io.quarkus.tls.runtime.keystores;

import static io.quarkus.tls.runtime.config.TlsConfigUtils.read;

import java.io.UncheckedIOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import io.quarkus.tls.runtime.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.runtime.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.config.JKSKeyStoreConfig;
import io.quarkus.tls.runtime.config.JKSTrustStoreConfig;
import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.KeyStoreCredentialProviderConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.quarkus.tls.runtime.config.TrustStoreCredentialProviderConfig;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;

/**
 * A utility class to validate JKS key store and trust store configurations.
 */
public class JKSKeyStores {

    private JKSKeyStores() {
        // Avoid direct instantiation
    }

    public static KeyStoreAndKeyCertOptions verifyJKSKeyStore(KeyStoreConfig config, Vertx vertx, String name) {
        var jksKeyStoreConfig = config.jks().orElseThrow();
        JksOptions options = toOptions(jksKeyStoreConfig, config.credentialsProvider(), name);
        KeyStore ks = loadKeyStore(vertx, name, options, "key");
        verifyKeyStoreAlias(options, name, ks);
        return new KeyStoreAndKeyCertOptions(ks, options);
    }

    public static TrustStoreAndTrustOptions verifyJKSTrustStoreStore(TrustStoreConfig config, Vertx vertx,
            String name) {
        JKSTrustStoreConfig jksConfig = config.jks().orElseThrow();
        JksOptions options = toOptions(jksConfig, config.credentialsProvider(), name);
        KeyStore ks = loadKeyStore(vertx, name, options, "trust");
        verifyTrustStoreAlias(options, name, ks);
        if (config.certificateExpirationPolicy() == TrustStoreConfig.CertificateExpiryPolicy.IGNORE) {
            return new TrustStoreAndTrustOptions(ks, options);
        } else {
            var wrapped = new ExpiryTrustOptions(options, config.certificateExpirationPolicy());
            return new TrustStoreAndTrustOptions(ks, wrapped);
        }

    }

    private static JksOptions toOptions(JKSKeyStoreConfig config,
            KeyStoreCredentialProviderConfig keyStoreCredentialProviderConfig, String name) {
        JksOptions options = new JksOptions();
        try {
            options.setValue(Buffer.buffer(read(config.path())));
            String p = CredentialProviders.getKeyStorePassword(config.password(), keyStoreCredentialProviderConfig)
                    .orElse(null);
            if (p == null) {
                throw new IllegalArgumentException("Invalid JKS key store configuration for certificate '" + name
                        + "' - the key store password is not set and cannot be retrieved from the credential provider.");
            }
            options.setPassword(p);
            if (config.alias().isPresent()) {
                options.setAlias(config.alias().get());
            }
            String ap = CredentialProviders.getAliasPassword(config.aliasPassword(), keyStoreCredentialProviderConfig)
                    .orElse(null);
            options.setAliasPassword(ap);
            return options;
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid JKS key store configuration for certificate '" + name
                    + "' - cannot read the key store file '" + config.path() + "'", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JKS key store configuration for certificate '" + name + "'", e);
        }
    }

    private static JksOptions toOptions(JKSTrustStoreConfig config,
            TrustStoreCredentialProviderConfig trustStoreCredentialProviderConfig, String name) {
        JksOptions options = new JksOptions();
        try {
            options.setValue(Buffer.buffer(read(config.path())));
            String password = CredentialProviders.getTrustStorePassword(config.password(), trustStoreCredentialProviderConfig)
                    .orElse(null);
            if (password == null) {
                throw new IllegalStateException("Invalid JKS trust store configuration for certificate '" + name
                        + "' - the trust store password is not set and cannot be retrieved from the credential provider.");
            }
            options.setPassword(password);
            if (config.alias().isPresent()) {
                options.setAlias(config.alias().get());
            }
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid JKS trust store configuration for certificate '" + name
                    + "' - cannot read the trust store file '" + config.path() + "'", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JKS trust store configuration for certificate '" + name + "'", e);
        }
        return options;
    }

    private static void verifyKeyStoreAlias(JksOptions options, String name, KeyStore ks) {
        String alias = options.getAlias();
        // Credential provider already called.
        String aliasPassword = options.getAliasPassword();
        if (alias != null) {
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in JKS key store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in JKS key store '" + name + "'", e);
            }

            char[] ap = null;
            if (aliasPassword != null) {
                ap = aliasPassword.toCharArray();
            }

            try {
                if (ks.getKey(alias, ap) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in JKS key store (private key not found)'" + name + "'");
                }
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in JKS key store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in JKS key store '" + name + "'", e);
            } catch (UnrecoverableKeyException e) {
                throw new IllegalArgumentException(
                        "Unable to recover the key for alias '" + alias + "' in JKS key store '" + name + "'", e);
            }
        }
    }

    private static void verifyTrustStoreAlias(JksOptions options, String name, KeyStore ks) {
        String alias = options.getAlias();
        if (alias != null) {
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in JKS trust store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in JKS trust store '" + name + "'", e);
            }
        }
    }

    private static KeyStore loadKeyStore(Vertx vertx, String name, JksOptions options, String type) {
        try {
            return options.loadKeyStore(vertx);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load JKS " + type + " store '" + name + "', verify the password.", e);
        }
    }
}
