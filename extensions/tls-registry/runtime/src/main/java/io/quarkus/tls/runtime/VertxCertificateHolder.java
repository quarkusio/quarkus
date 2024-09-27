package io.quarkus.tls.runtime;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.quarkus.tls.runtime.keystores.TrustAllOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

public class VertxCertificateHolder implements TlsConfiguration {

    private final TlsBucketConfig config;
    private final List<Buffer> crls;
    private TrustOptions trustOptions;
    private KeyStore trustStore;
    private KeyCertOptions keyStoreOptions;
    private KeyStore keyStore;

    private final Vertx vertx;
    private final String name;

    VertxCertificateHolder(Vertx vertx, String name, TlsBucketConfig config, KeyStoreAndKeyCertOptions ks,
            TrustStoreAndTrustOptions ts) {
        this.config = config;
        this.vertx = vertx;
        this.name = name;
        if (ks != null) {
            keyStoreOptions = ks.options;
            keyStore = ks.keyStore;
        } else {
            keyStoreOptions = null;
            keyStore = null;
        }
        if (ts != null) {
            trustOptions = ts.options;
            trustStore = ts.trustStore;
        } else {
            trustOptions = null;
            trustStore = null;
        }

        crls = new ArrayList<>();
        if (config().certificateRevocationList().isPresent()) {
            for (Path path : config().certificateRevocationList().get()) {
                byte[] bytes = TlsConfigUtils.read(path);
                crls.add(Buffer.buffer(bytes));
            }
        }
    }

    @Override
    public synchronized KeyCertOptions getKeyStoreOptions() {
        return keyStoreOptions;
    }

    @Override
    public synchronized KeyStore getKeyStore() {
        return keyStore;
    }

    @Override
    public synchronized TrustOptions getTrustStoreOptions() {
        return trustOptions;
    }

    @Override
    public synchronized KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public synchronized SSLContext createSSLContext() throws Exception {
        KeyManagerFactory keyManagerFactory;
        KeyManager[] keyManagers = null;
        if (keyStoreOptions != null) {
            keyManagerFactory = keyStoreOptions.getKeyManagerFactory(vertx);
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        TrustManagerFactory trustManagerFactory;
        TrustManager[] trustManagers = null;
        if (trustOptions != null) {
            trustManagerFactory = trustOptions.getTrustManagerFactory(vertx);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    @Override
    public synchronized SSLOptions getSSLOptions() {
        SSLOptions options = new SSLOptions();
        options.setKeyCertOptions(getKeyStoreOptions());
        options.setTrustOptions(getTrustStoreOptions());
        options.setUseAlpn(config().alpn());
        options.setSslHandshakeTimeoutUnit(TimeUnit.SECONDS);
        options.setSslHandshakeTimeout(config().handshakeTimeout().toSeconds());
        options.setEnabledSecureTransportProtocols(config().protocols());

        for (Buffer buffer : crls) {
            options.addCrlValue(buffer);
        }

        for (String cipher : config().cipherSuites().orElse(Collections.emptyList())) {
            options.addEnabledCipherSuite(cipher);
        }

        return options;
    }

    @Override
    public boolean isTrustAll() {
        return config().trustAll();
    }

    @Override
    public Optional<String> getHostnameVerificationAlgorithm() {
        return config.hostnameVerificationAlgorithm();
    }

    @Override
    public boolean usesSni() {
        if (config.keyStore().isPresent()) {
            return config.keyStore().get().sni();
        }
        return false;
    }

    @Override
    public boolean reload() {
        if (keyStore == null && trustStore == null) {
            return false;
        }

        KeyStoreAndKeyCertOptions keyStoreUpdateResult = null;
        TrustStoreAndTrustOptions trustStoreUpdateResult = null;
        // Reload keystore
        if (keyStore != null) {
            try {
                keyStoreUpdateResult = CertificateRecorder.verifyKeyStore(config.keyStore().orElseThrow(), vertx, name);
            } catch (Exception e) {
                return false;
            }
        }

        // Reload truststore
        if (trustStore != null) {
            try {
                trustStoreUpdateResult = CertificateRecorder.verifyTrustStore(config.trustStore().orElseThrow(), vertx, name);
            } catch (Exception e) {
                return false;
            }
        } else if (config.trustAll()) {
            trustStoreUpdateResult = new TrustStoreAndTrustOptions(null, TrustAllOptions.INSTANCE);
        }

        if (keyStoreUpdateResult == null && trustStoreUpdateResult == null) {
            return false;
        }

        // Also reload the revoked certificates
        List<Buffer> newCRLs = new ArrayList<>();
        if (config().certificateRevocationList().isPresent()) {
            for (Path path : config().certificateRevocationList().get()) {
                byte[] bytes = TlsConfigUtils.read(path);
                newCRLs.add(Buffer.buffer(bytes));
            }
        }

        synchronized (this) {
            keyStoreOptions = keyStoreUpdateResult != null ? keyStoreUpdateResult.options : null;
            keyStore = keyStoreUpdateResult != null ? keyStoreUpdateResult.keyStore : null;
            trustOptions = trustStoreUpdateResult != null ? trustStoreUpdateResult.options : null;
            trustStore = trustStoreUpdateResult != null ? trustStoreUpdateResult.trustStore : null;
            crls.clear();
            crls.addAll(newCRLs);
        }
        return true;
    }

    public TlsBucketConfig config() {
        return config;
    }

}
