package io.quarkus.amazon.common.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.http.TlsTrustManagersProvider;

/**
 * A TlsTrustManagersProvider for creating trustmanagers from a file-based truststore
 */
public class FileStoreTlsTrustManagersProvider implements TlsTrustManagersProvider {

    private final Path path;
    private final String type;
    private final char[] password;

    public FileStoreTlsTrustManagersProvider(FileStoreTlsManagersProviderConfig fileStore) {
        path = fileStore.path.get();
        type = fileStore.type.get();
        password = fileStore.password.map(String::toCharArray).orElse(null);
    }

    @Override
    public TrustManager[] trustManagers() {
        try (InputStream storeInputStream = Files.newInputStream(path)) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(storeInputStream, password);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            return tmf.getTrustManagers();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            LOGGER.error("Failed to load truststore", e);
            return null;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreTlsTrustManagersProvider.class);
}
