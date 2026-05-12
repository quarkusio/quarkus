package io.quarkus.tls.runtime.keystores;

import static io.quarkus.tls.runtime.config.TlsConfigUtils.read;

import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.util.Optional;

import io.quarkus.tls.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.OtherKeyStoreConfig;
import io.quarkus.tls.runtime.config.OtherTrustStoreConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptions;

/**
 * A utility class to load key stores and trust stores with arbitrary types.
 */
public class OtherKeyStores {

    private OtherKeyStores() {
        // Avoid direct instantiation
    }

    public static KeyStoreAndKeyCertOptions verifyOtherKeyStore(KeyStoreConfig ksc, String name) {
        OtherKeyStoreConfig config = ksc.other().orElseThrow();

        if (config.path().isEmpty()) {
            throw new IllegalStateException("Invalid key store configuration for certificate '" + name
                    + "' - no path specified and no KeyStoreFactory found for type '" + config.type() + "'");
        }

        try {
            byte[] data = read(config.path().get());
            String password = CredentialProviders.getKeyStorePassword(config.password(), ksc.credentialsProvider())
                    .orElse(null);
            if (password == null) {
                throw new IllegalStateException("Invalid key store configuration for certificate '" + name
                        + "' - the key store password is not set and cannot be retrieved from the credential provider.");
            }

            KeyStore ks = getInstance(config.type(), config.provider());
            ks.load(new ByteArrayInputStream(data), password.toCharArray());

            KeyStoreOptions options = new KeyStoreOptions();
            options.setType(config.type());
            if (config.provider().isPresent()) {
                options.setProvider(config.provider().get());
            }
            options.setValue(Buffer.buffer(data));
            options.setPassword(password);
            if (config.alias().isPresent()) {
                options.setAlias(config.alias().get());
            }
            String aliasPassword = CredentialProviders.getAliasPassword(config.aliasPassword(), ksc.credentialsProvider())
                    .orElse(null);
            options.setAliasPassword(aliasPassword);

            verifyKeyStoreAlias(config, name, ks, aliasPassword);
            return new KeyStoreAndKeyCertOptions(ks, options);
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid key store configuration for certificate '" + name
                    + "' - cannot read the key store file '" + config.path().get() + "'", e);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid key store configuration for certificate '" + name + "'", e);
        }
    }

    public static TrustStoreAndTrustOptions verifyOtherTrustStore(TrustStoreConfig tsc, String name) {
        OtherTrustStoreConfig config = tsc.other().orElseThrow();

        if (config.path().isEmpty()) {
            throw new IllegalStateException("Invalid trust store configuration for certificate '" + name
                    + "' - no path specified and no TrustStoreFactory found for type '" + config.type() + "'");
        }

        try {
            byte[] data = read(config.path().get());
            String password = CredentialProviders.getTrustStorePassword(config.password(), tsc.credentialsProvider())
                    .orElse(null);
            if (password == null) {
                throw new IllegalStateException("Invalid trust store configuration for certificate '" + name
                        + "' - the trust store password is not set and cannot be retrieved from the credential provider.");
            }

            KeyStore ks = getInstance(config.type(), config.provider());
            ks.load(new ByteArrayInputStream(data), password.toCharArray());

            KeyStoreOptions options = new KeyStoreOptions();
            options.setType(config.type());
            if (config.provider().isPresent()) {
                options.setProvider(config.provider().get());
            }
            options.setValue(Buffer.buffer(data));
            options.setPassword(password);
            if (config.alias().isPresent()) {
                options.setAlias(config.alias().get());
            }

            verifyTrustStoreAlias(config.alias(), name, ks);

            if (tsc.certificateExpirationPolicy() == TrustStoreConfig.CertificateExpiryPolicy.IGNORE) {
                return new TrustStoreAndTrustOptions(ks, options);
            } else {
                var wrapped = new ExpiryTrustOptions(options, tsc.certificateExpirationPolicy());
                return new TrustStoreAndTrustOptions(ks, wrapped);
            }
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid trust store configuration for certificate '" + name
                    + "' - cannot read the trust store file '" + config.path().get() + "'", e);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid trust store configuration for certificate '" + name + "'", e);
        }
    }

    private static KeyStore getInstance(String type, Optional<String> provider) {
        try {
            if (provider.isPresent()) {
                return KeyStore.getInstance(type, provider.get());
            }
            return KeyStore.getInstance(type);
        } catch (KeyStoreException | NoSuchProviderException e) {
            throw new IllegalStateException("Unable to create key store of type '" + type + "'"
                    + (provider.isPresent() ? " with provider '" + provider.get() + "'" : ""), e);
        }
    }

    private static void verifyKeyStoreAlias(OtherKeyStoreConfig config, String name, KeyStore ks,
            String aliasPassword) {
        if (config.alias().isPresent()) {
            String alias = config.alias().get();
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in key store (certificate not found) '" + name + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in key store '" + name + "'", e);
            }

            char[] ap = aliasPassword != null ? aliasPassword.toCharArray() : null;
            try {
                if (ks.getKey(alias, ap) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in key store (private key not found) '" + name + "'");
                }
            } catch (KeyStoreException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in key store '" + name + "'", e);
            } catch (UnrecoverableKeyException e) {
                throw new IllegalArgumentException(
                        "Unable to recover the key for alias '" + alias + "' in key store '" + name + "'", e);
            }
        }
    }

    private static void verifyTrustStoreAlias(Optional<String> maybeAlias, String name, KeyStore ks) {
        if (maybeAlias.isPresent()) {
            String alias = maybeAlias.get();
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in trust store (certificate not found) '" + name + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException(
                        "Unable to verify alias '" + alias + "' in trust store '" + name + "'", e);
            }
        }
    }
}
