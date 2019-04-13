package io.quarkus.runtime.configuration.ssl;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;

import org.jboss.logging.Logger;
import org.wildfly.common.iteration.CodePointIterator;
import org.wildfly.security.pem.Pem;
import org.wildfly.security.pem.PemEntry;
import org.wildfly.security.ssl.CipherSuiteSelector;
import org.wildfly.security.ssl.Protocol;
import org.wildfly.security.ssl.ProtocolSelector;
import org.wildfly.security.ssl.SSLContextBuilder;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Shared configuration for setting up server-side SSL.
 */
@ConfigGroup
public class ServerSslConfig {
    /**
     * The server certificate configuration.
     */
    public CertificateConfig certificate;

    /**
     * The cipher suites to use. If none is given, a reasonable default is selected.
     */
    @ConfigItem
    public Optional<CipherSuiteSelector> cipherSuites;

    /**
     * The list of protocols to explicitly enable.
     */
    @ConfigItem(defaultValue = "TLSv1.3,TLSv1.2")
    public List<Protocol> protocols;

    /**
     * The SSL provider name to use. If none is given, the platform default is used.
     */
    @ConfigItem
    public Optional<String> providerName;

    /**
     * The SSL session cache size. If not given, the platform default is used.
     */
    @ConfigItem
    public OptionalInt sessionCacheSize;

    /**
     * The SSL session cache timeout. If not given, the platform default is used.
     */
    @ConfigItem
    public Optional<Duration> sessionTimeout;

    /**
     * Get an {@code SSLContext} for this server configuration.
     *
     * @return the {@code SSLContext}, or {@code null} if SSL should not be configured
     * @throws GeneralSecurityException if something failed in the context setup
     */
    public SSLContext toSSLContext() throws GeneralSecurityException, IOException {
        //TODO: static fields break config
        Logger log = Logger.getLogger("io.quarkus.configuration.ssl");
        final Optional<Path> certFile = certificate.file;
        final Optional<Path> keyFile = certificate.keyFile;
        final Optional<Path> keyStoreFile = certificate.keyStoreFile;
        final String keystorePassword = certificate.keyStorePassword;
        final KeyStore keyStore;
        if (certFile.isPresent() && keyFile.isPresent()) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, keystorePassword.toCharArray());
            final Path certPath = certFile.get();
            final Iterator<PemEntry<?>> certItr = Pem.parsePemContent(load(certPath));
            final ArrayList<X509Certificate> certList = new ArrayList<>();
            while (certItr.hasNext()) {
                final PemEntry<?> item = certItr.next();
                final X509Certificate cert = item.tryCast(X509Certificate.class);
                if (cert != null) {
                    certList.add(cert);
                } else {
                    log.warnf("Ignoring non-certificate in certificate file \"%s\" (the type was %s)", certPath,
                            item.getEntry().getClass());
                }
            }
            if (certList.isEmpty()) {
                log.warnf("No certificate found in file \"%s\"", certPath);
            }
            final Path keyPath = keyFile.get();
            final Iterator<PemEntry<?>> keyItr = Pem.parsePemContent(load(keyPath));
            final PrivateKey privateKey;
            for (;;) {
                if (!keyItr.hasNext()) {
                    log.warnf("No key found in file \"%s\"", keyPath);
                    return null;
                }
                final PemEntry<?> next = keyItr.next();
                final PrivateKey entryKey = next.tryCast(PrivateKey.class);
                if (entryKey != null) {
                    privateKey = entryKey;
                    break;
                }
                log.warnf("Ignoring non-key in key file \"%s\" (the type was %s)", keyPath, next.getEntry().getClass());
            }
            if (keyItr.hasNext()) {
                log.warnf("Ignoring extra content in key file \"%s\"", keyPath);
            }
            //Entry password needs to match the keystore password
            keyStore.setEntry("default", new KeyStore.PrivateKeyEntry(privateKey, certList.toArray(new X509Certificate[0])),
                    new KeyStore.PasswordProtection(keystorePassword.toCharArray()));
        } else if (keyStoreFile.isPresent()) {
            final Path keyStorePath = keyStoreFile.get();
            final Optional<String> keyStoreFileType = certificate.keyStoreFileType;
            final String type;
            if (keyStoreFileType.isPresent()) {
                type = keyStoreFileType.get();
            } else {
                final String pathName = keyStorePath.toString();
                if (pathName.endsWith(".jks")) {
                    type = "jks";
                } else if (pathName.endsWith(".jceks")) {
                    type = "jceks";
                } else if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
                    type = "pkcs12";
                } else {
                    // not sure, just guess
                    type = "jks";
                }
            }
            keyStore = KeyStore.getInstance(type);
            try (InputStream is = Files.newInputStream(keyStorePath)) {
                keyStore.load(is, null);
            }
        } else {
            return null;
        }
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
        final SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.setCipherSuiteSelector(cipherSuites.orElse(CipherSuiteSelector.openSslDefault()));
        ProtocolSelector protocolSelector;
        if (protocols.isEmpty()) {
            protocolSelector = ProtocolSelector.defaultProtocols();
        } else {
            protocolSelector = ProtocolSelector.empty().add(protocols.toArray(new Protocol[0]));
        }
        sslContextBuilder.setProtocolSelector(protocolSelector);
        sslContextBuilder.setKeyManager((X509ExtendedKeyManager) keyManagerFactory.getKeyManagers()[0]);
        if (sessionCacheSize.isPresent()) {
            sslContextBuilder.setSessionCacheSize(sessionCacheSize.getAsInt());
        }
        if (sessionTimeout.isPresent()) {
            sslContextBuilder.setSessionTimeout((int) min(Integer.MAX_VALUE, sessionTimeout.get().getSeconds()));
        }
        if (providerName.isPresent()) {
            sslContextBuilder.setProviderName(providerName.get());
        }
        return sslContextBuilder.build().create();
    }

    static CodePointIterator load(final Path path) throws IOException {
        final int size = Math.toIntExact(Files.size(path));
        char[] chars = new char[size];
        int c = 0;
        try (InputStream is = Files.newInputStream(path)) {
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                while (c < size) {
                    final int res = isr.read(chars, c, size - c);
                    if (res == -1)
                        break;
                    c += res;
                }
            }
        }
        return CodePointIterator.ofChars(chars, 0, c);
    }
}
