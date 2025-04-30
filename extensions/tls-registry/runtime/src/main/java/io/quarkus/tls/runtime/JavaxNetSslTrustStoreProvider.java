package io.quarkus.tls.runtime;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.quarkus.tls.runtime.config.TrustStoreConfig.CertificateExpiryPolicy;
import io.quarkus.tls.runtime.keystores.ExpiryTrustOptions;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;
import io.vertx.core.net.impl.KeyStoreHelper;

/**
 * Provides {@link TrustStoreAndTrustOptions} wrapping the default Java trust store specified as follows:
 * <ol>
 * <li>If the {@code javax.net.ssl.trustStore} property is defined, then it is honored
 * <li>If the {@code $JAVA_HOME/lib/security/jssecacerts} is a regular file, then it is used
 * <li>If the {@code $JAVA_HOME/lib/security/cacerts} is a regular file, then it is used
 * </ol>
 * <p>
 * For native image, be aware that the default trust material is stored inside the native image.
 * Therefore it is not loaded anew at application start unless the application is started with
 * {@code -Djavax.net.ssl.trustStore=path/to/trust-store} - see
 * <a href="https://www.graalvm.org/latest/reference-manual/native-image/dynamic-features/CertificateManagement/">Certificate
 * Management</a> in GraalVM reference manual.
 *
 * @since 3.18.0
 */
public class JavaxNetSslTrustStoreProvider {

    public static TrustStoreAndTrustOptions getTrustStore(Vertx vertx) {
        JavaNetSslTrustOptions options = new JavaNetSslTrustOptions();
        return new TrustStoreAndTrustOptions(options.keystore, new ExpiryTrustOptions(options, CertificateExpiryPolicy.WARN));
    }

    static class JavaNetSslTrustOptions implements TrustOptions {
        private final TrustManagerFactory trustManagerFactory;
        private final KeyStore keystore;
        private KeyStoreHelper helper;

        JavaNetSslTrustOptions() {
            try {
                final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);

                final KeyStore cacerts = copyCerts(tmf);
                trustManagerFactory = tmf;
                keystore = cacerts;
            } catch (NoSuchAlgorithmException | KeyStoreException | InvalidNameException | CertificateException
                    | IOException e) {
                throw new RuntimeException(e);
            }
        }

        static KeyStore copyCerts(final TrustManagerFactory tmf)
                throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, InvalidNameException {
            final String tsType = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());
            final KeyStore cacerts = KeyStore.getInstance(tsType);
            cacerts.load(null, null);
            final Set<String> aliases = new HashSet<>();
            for (TrustManager tm : tmf.getTrustManagers()) {
                for (X509Certificate c : ((X509TrustManager) tm).getAcceptedIssuers()) {
                    final String dn = c.getSubjectX500Principal().getName();
                    final List<Rdn> rdns = new LdapName(dn).getRdns();
                    String alias = rdns.stream()
                            .filter(rdn -> rdn.getType().equalsIgnoreCase("cn"))
                            .map(rdn -> rdn.getValue().toString())
                            .findFirst()
                            .orElseGet(() -> rdns.stream()
                                    .filter(rdn -> rdn.getType().equalsIgnoreCase("ou"))
                                    .map(rdn -> rdn.getValue().toString())
                                    .findFirst()
                                    .orElse(dn));
                    alias = alias.replace(" ", "");
                    alias = alias.toLowerCase(Locale.ROOT);
                    if (aliases.contains(alias)) {
                        /* Make the alias unique if needed */
                        int i = 1;
                        String indexedAlias = alias + i;
                        while (aliases.contains(indexedAlias)) {
                            indexedAlias = alias + (++i);
                        }
                        alias = indexedAlias;
                    }
                    aliases.add(alias);
                    cacerts.setCertificateEntry(alias, c);
                }
            }
            return cacerts;
        }

        @Override
        public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) throws Exception {
            if (helper == null) {
                final String cacertsPassword = System.getProperty("javax.net.ssl.trustStorePassword", "changeit");
                helper = new KeyStoreHelper(keystore, cacertsPassword, null);
            }
            return helper::getTrustMgr;
        }

        @Override
        public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
            return trustManagerFactory;
        }

        @Override
        public TrustOptions copy() {
            return this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((keystore == null) ? 0 : keystore.hashCode());
            return result;
        }

    }

}
