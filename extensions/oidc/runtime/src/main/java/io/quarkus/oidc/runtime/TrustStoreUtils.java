package io.quarkus.oidc.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jose4j.keys.X509Util;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.runtime.util.ClassPathUtils;

public class TrustStoreUtils {
    private static final Logger LOGGER = Logger.getLogger(TrustStoreUtils.class.getName());

    public static Set<String> getTrustedCertificateThumbprints(Path path, String trustStoreSecret,
            Optional<String> trustStoreCertAlias, Optional<String> trustStoreFileType) {
        URL trustStoreFileUrl = null;
        if ((trustStoreFileUrl = Thread.currentThread().getContextClassLoader()
                .getResource(ClassPathUtils.toResourceName(path))) != null) {
            return readTrustStore(path, trustStoreFileUrl, trustStoreSecret, trustStoreCertAlias, trustStoreFileType);
        } else if (Files.exists(path)) {
            try {
                return readTrustStore(path, path.toUri().toURL(), trustStoreSecret, trustStoreCertAlias, trustStoreFileType);
            } catch (MalformedURLException e) {
                LOGGER.errorf("Keystore %s location is not a valid URL", path.toUri());
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.errorf("Keystore %s can not be found on the classpath and the file system", path.toUri());
            throw new RuntimeException();
        }
    }

    private static Set<String> readTrustStore(Path path, URL trustStoreFileUrl, String trustStoreSecret,
            Optional<String> trustStoreCertAlias, Optional<String> trustStoreFileType) {

        try (InputStream fis = trustStoreFileUrl.openStream()) {
            KeyStore keyStore = KeyStore.getInstance(OidcCommonUtils.getKeyStoreType(trustStoreFileType, path));
            keyStore.load(fis, trustStoreSecret.toCharArray());

            Set<String> allThumbprints = new HashSet<>();
            if (trustStoreCertAlias.isPresent()) {
                addThumbprints(keyStore, allThumbprints, trustStoreCertAlias.get());
            } else {
                for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                    addThumbprints(keyStore, allThumbprints, aliases.nextElement());
                }
            }

            if (allThumbprints.isEmpty()) {
                LOGGER.errorf("Keystore %s entries can not be loaded", trustStoreFileUrl.toString());
                throw new RuntimeException();
            }

            return allThumbprints;
        } catch (IOException e) {
            LOGGER.errorf("Keystore %s can not be loaded", trustStoreFileUrl.toString());
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOGGER.errorf("Keystore %s entries can not be loaded", trustStoreFileUrl.toString());
            throw new RuntimeException(e);
        }
    }

    private static void addThumbprints(KeyStore keyStore, Set<String> allThumbprints, String alias)
            throws Exception {
        Entry entry = keyStore.getEntry(alias, null);
        if (entry instanceof TrustedCertificateEntry) {
            X509Certificate cert = (X509Certificate) ((TrustedCertificateEntry) entry).getTrustedCertificate();
            allThumbprints.add(calculateThumprint(cert));
        }
    }

    public static String calculateThumprint(X509Certificate cert) {
        return X509Util.x5tS256(cert);
    }
}