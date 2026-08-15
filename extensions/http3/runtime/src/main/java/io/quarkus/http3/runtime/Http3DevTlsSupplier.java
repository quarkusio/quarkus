package io.quarkus.http3.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ServerSSLOptions;

public record Http3DevTlsSupplier(String keystorePath, String password) implements Supplier<TlsConfiguration> {

    @Override
    public TlsConfiguration get() {
        KeyStore ks = loadKeyStore();
        PfxOptions pfxOptions = new PfxOptions()
                .setPath(keystorePath)
                .setPassword(password);

        return new BaseTlsConfiguration() {
            @Override
            public KeyStore getKeyStore() {
                return ks;
            }

            @Override
            public KeyCertOptions getKeyStoreOptions() {
                return pfxOptions;
            }

            @Override
            public ServerSSLOptions getServerSSLOptions() {
                ServerSSLOptions options = new ServerSSLOptions();
                options.setKeyCertOptions(pfxOptions);
                return options;
            }

            @Override
            public String getName() {
                return "http3-dev";
            }
        };
    }

    private KeyStore loadKeyStore() {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream is = Files.newInputStream(Path.of(keystorePath))) {
                ks.load(is, password.toCharArray());
            }
            return ks;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to load auto-generated HTTP/3 dev keystore from " + keystorePath, e);
        }
    }
}
