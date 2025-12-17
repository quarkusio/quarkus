package io.quarkus.tls.runtime;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.jboss.logging.Logger;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

public class VertxCertificateHolder implements TlsConfiguration {

    private static final Logger LOGGER = Logger.getLogger(VertxCertificateHolder.class.getName());

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

        warnIfOldProtocols(options.getEnabledSecureTransportProtocols(), name);

        for (Buffer buffer : crls) {
            options.addCrlValue(buffer);
        }

        for (String cipher : config().cipherSuites().orElse(Collections.emptyList())) {
            options.addEnabledCipherSuite(cipher);
        }

        return options;
    }

    boolean warnIfOldProtocols(Set<String> protocols, String name) {
        var list = protocols.stream().map(String::toLowerCase).map(String::trim).toList();

        // Quick check to skip the warning if only the default protocol is used.
        if (list.size() == 1 && list.get(0).equalsIgnoreCase(TlsBucketConfig.DEFAULT_TLS_PROTOCOLS)) {
            return false;
        }

        boolean warned = false;

        // Check for SSL protocols
        if (list.stream().anyMatch(p -> p.startsWith("ssl"))) {
            LOGGER.warnf("Insecure SSL protocol is enabled in TLS bucket '%s'." +
                    " It is strongly recommended to disable SSL protocols (SSLv2, SSLv3), and use at least TLSv1.3.", name);
            warned = true;
        }

        // Check for old TLS protocols (TLSv1.0, TLSv1.1)
        if (list.contains("tlsv1") || list.contains("tlsv1.1")) {
            LOGGER.warnf("Insecure TLS protocol TLSv1.0 or TLSv1.1 is enabled in TLS bucket '%s'." +
                    " It is strongly recommended to disable TLSv1.0 and TLSv1.1, and use at least TLSv1.3.", name);
            warned = true;
        }

        // Check if TLSv1.3 or higher is enabled.
        boolean isUsingModernTlsVersion = false;
        for (String p : list) {
            if (isRecentOrFutureTLSVersion(p)) {
                isUsingModernTlsVersion = true;
                break;
            }
        }

        if (!isUsingModernTlsVersion) {
            LOGGER.warnf("TLSv1.3 or higher protocol is not enabled in TLS bucket '%s'." +
                    " It is *strongly* recommended to enable TLSv1.3 or higher.", name);
            warned = true;
        }

        return warned;
    }

    /**
     * Checks if a protocol string represents TLS 1.3 or higher.
     *
     * @param protocol the protocol string (already lowercased and trimmed)
     * @return true if the protocol is TLSv1.3 or higher
     */
    private boolean isRecentOrFutureTLSVersion(String protocol) {
        if (!protocol.startsWith("tlsv")) {
            return false;
        }

        String afterTlsv = protocol.substring(4); // Skip "tlsv"
        if (afterTlsv.isEmpty()) {
            return false;
        }

        char majorChar = afterTlsv.charAt(0);

        // Check for TLSv1.x
        if (afterTlsv.startsWith("1.")) {
            String minorVersion = afterTlsv.substring(2); // Everything after "1."
            if (minorVersion.isEmpty()) {
                return false;
            }

            // Single digit 3-9
            if (minorVersion.length() == 1) {
                char minorChar = minorVersion.charAt(0);
                return minorChar >= '3' && minorChar <= '9'; // TLSv1.3 to TLSv1.9
            } else {
                return true; // Multi-digit (TLSv1.10, TLSv1.11, etc.)
            }
        }

        // TLSv2.x, TLSv3.x, etc.
        return majorChar >= '2' && majorChar <= '9'; // Future-proof for TLSv2, TLSv3, etc, if they ever come.
    }

    @Override
    public boolean isTrustAll() {
        return config().trustAll() || getTrustStoreOptions() == TrustAllOptions.INSTANCE;
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
                keyStoreUpdateResult = CertificateRecorder.getKeyStore(config, vertx, name);
            } catch (Exception e) {
                return false;
            }
        }

        // Reload truststore
        if (trustStore != null) {
            try {
                trustStoreUpdateResult = CertificateRecorder.getTrustStore(config, vertx, name);
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

    @Override
    public String getName() {
        return name;
    }

    public TlsBucketConfig config() {
        return config;
    }

}
