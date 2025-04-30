package io.quarkus.tls.runtime.keystores;

import static io.quarkus.tls.runtime.config.TlsConfigUtils.read;

import java.io.UncheckedIOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Optional;

import io.quarkus.tls.runtime.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.runtime.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.KeyStoreCredentialProviderConfig;
import io.quarkus.tls.runtime.config.P12KeyStoreConfig;
import io.quarkus.tls.runtime.config.P12TrustStoreConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.quarkus.tls.runtime.config.TrustStoreCredentialProviderConfig;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PfxOptions;

/**
 * A utility class to validate P12 key store and trust store configurations.
 */
public class P12KeyStores {

    private P12KeyStores() {
        // Avoid direct instantiation
    }

    public static KeyStoreAndKeyCertOptions verifyP12KeyStore(KeyStoreConfig ksc, Vertx vertx, String name) {
        P12KeyStoreConfig config = ksc.p12().orElseThrow();
        PfxOptions options = toOptions(config, ksc.credentialsProvider(), name);
        KeyStore ks = loadKeyStore(vertx, name, options, "key");
        verifyKeyStoreAlias(options, name, ks);
        return new KeyStoreAndKeyCertOptions(ks, options);
    }

    public static TrustStoreAndTrustOptions verifyP12TrustStoreStore(TrustStoreConfig config, Vertx vertx, String name) {
        P12TrustStoreConfig p12Config = config.p12().orElseThrow();
        PfxOptions options = toOptions(p12Config, config.credentialsProvider(), name);
        KeyStore ks = loadKeyStore(vertx, name, options, "trust");
        verifyTrustStoreAlias(p12Config.alias(), name, ks);
        if (config.certificateExpirationPolicy() == TrustStoreConfig.CertificateExpiryPolicy.IGNORE) {
            return new TrustStoreAndTrustOptions(ks, options);
        } else {
            var wrapped = new ExpiryTrustOptions(options, config.certificateExpirationPolicy());
            return new TrustStoreAndTrustOptions(ks, wrapped);
        }
    }

    private static PfxOptions toOptions(P12KeyStoreConfig config, KeyStoreCredentialProviderConfig pc, String name) {
        PfxOptions options = new PfxOptions();
        try {
            options.setValue(Buffer.buffer(read(config.path())));
            String password = CredentialProviders.getKeyStorePassword(config.password(), pc)
                    .orElse(null);
            if (password == null) {
                throw new IllegalStateException("Invalid P12 key store configuration for certificate '" + name
                        + "' - the key store password is not set and cannot be retrieved from the credential provider.");
            }
            options.setPassword(password);
            if (config.alias().isPresent()) {
                options.setAlias(config.alias().get());
            }
            String ap = CredentialProviders.getAliasPassword(config.aliasPassword(), pc).orElse(null);
            options.setAliasPassword(ap);
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid P12 key store configuration for certificate '" + name
                    + "' - cannot read the key store file '" + config.path() + "'", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid P12 key store configuration for certificate '" + name + "'", e);
        }
        return options;
    }

    private static PfxOptions toOptions(P12TrustStoreConfig config, TrustStoreCredentialProviderConfig cp, String name) {
        PfxOptions options = new PfxOptions();
        try {
            options.setValue(Buffer.buffer(read(config.path())));
            String password = CredentialProviders.getTrustStorePassword(config.password(), cp)
                    .orElse(null);
            if (password == null) {
                throw new IllegalStateException("Invalid P12 trust store configuration for certificate '" + name
                        + "' - the trust store password is not set and cannot be retrieved from the credential provider.");
            }
            options.setPassword(password);
            if (config.alias().isPresent()) {
                options.setAlias(config.alias().get());
            }
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid P12 trust store configuration for certificate '" + name
                    + "' - cannot read the trust store file '" + config.path() + "'", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid P12 trust store configuration for certificate '" + name + "'", e);
        }
        return options;
    }

    private static void verifyKeyStoreAlias(PfxOptions options, String name,
            KeyStore ks) {
        String alias = options.getAlias();
        String aliasPassword = options.getAliasPassword();
        if (alias != null) {
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in P12 key store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in P12 key store '" + name + "'", e);
            }

            char[] pwd = null;
            if (aliasPassword != null) {
                pwd = aliasPassword.toCharArray();
            }

            try {
                if (ks.getKey(alias, pwd) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in P12 key store (private key not found)'" + name + "'");
                }
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in P12 key store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in P12 key store '" + name + "'", e);
            } catch (UnrecoverableKeyException e) {
                throw new IllegalArgumentException(
                        "Unable to recover the key for alias '" + alias + "' in P12 key store '" + name + "'", e);
            }
        }
    }

    private static void verifyTrustStoreAlias(Optional<String> maybeAlias, String name, KeyStore ks) {
        if (maybeAlias.isPresent()) {
            String alias = maybeAlias.get();
            try {
                if (ks.getCertificate(alias) == null) {
                    throw new IllegalStateException(
                            "Alias '" + alias + "' not found in P12 trust store (certificate not found)'" + name + "'");
                }
            } catch (KeyStoreException e) {
                throw new IllegalStateException("Unable to verify alias '" + alias + "' in P12 trust store '" + name + "'", e);
            }
        }
    }

    private static KeyStore loadKeyStore(Vertx vertx, String name, PfxOptions options, String type) {
        KeyStore ks;
        try {
            ks = options.loadKeyStore(vertx);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load P12 " + type + " store '" + name + "', verify the password.", e);
        }
        return ks;
    }
}
