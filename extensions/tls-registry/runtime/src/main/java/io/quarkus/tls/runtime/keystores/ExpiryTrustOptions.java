package io.quarkus.tls.runtime.keystores;

import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.function.Function;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.logging.Logger;

import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.mutiny.unchecked.UncheckedFunction;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

/**
 * A trust options that verify for the certificate expiration date and reject the certificate if it is expired.
 */
public class ExpiryTrustOptions implements TrustOptions {

    private final TrustOptions delegate;
    private final TrustStoreConfig.CertificateExpiryPolicy policy;

    private static final Logger LOGGER = Logger.getLogger(ExpiryTrustOptions.class);

    public ExpiryTrustOptions(TrustOptions delegate, TrustStoreConfig.CertificateExpiryPolicy certificateExpiryPolicy) {
        this.delegate = delegate;
        this.policy = certificateExpiryPolicy;
    }

    public TrustOptions unwrap() {
        return delegate;
    }

    @Override
    public TrustOptions copy() {
        return this;
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory(Vertx vertx) throws Exception {
        var tmf = delegate.getTrustManagerFactory(vertx);
        return new TrustManagerFactory(new TrustManagerFactorySpi() {
            @Override
            protected void engineInit(KeyStore ks) throws KeyStoreException {
                tmf.init(ks);
            }

            @Override
            protected void engineInit(ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException {
                tmf.init(spec);
            }

            @Override
            protected TrustManager[] engineGetTrustManagers() {
                var managers = tmf.getTrustManagers();
                return getWrappedTrustManagers(managers);
            }
        }, tmf.getProvider(), tmf.getAlgorithm()) {
            // Empty - we use this pattern to have access to the protected constructor
        };
    }

    @Override
    public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) {
        return Unchecked.function(new UncheckedFunction<String, TrustManager[]>() {
            @Override
            public TrustManager[] apply(String s) throws Exception {
                TrustManager[] tms = delegate.trustManagerMapper(vertx).apply(s);
                return ExpiryTrustOptions.this.getWrappedTrustManagers(tms);
            }
        });
    }

    private TrustManager[] getWrappedTrustManagers(TrustManager[] tms) {
        // If we do not find any trust managers (for example in the SNI case, where we do not have a trust manager for
        // a given name), return `null` and not an empty array.
        if (tms == null) {
            return null;
        }
        var wrapped = new TrustManager[tms.length];
        for (int i = 0; i < tms.length; i++) {
            var manager = tms[i];
            if (manager instanceof X509ExtendedTrustManager) {
                wrapped[i] = new ExpiryAwareX509ExtendedTrustManager((X509ExtendedTrustManager) manager);
            } else if (manager instanceof X509TrustManager) {
                wrapped[i] = new ExpiryAwareX509TrustManager((X509TrustManager) manager);
            } else {
                wrapped[i] = manager;
            }
        }
        return wrapped;
    }

    private class ExpiryAwareX509ExtendedTrustManager extends X509ExtendedTrustManager {

        final X509ExtendedTrustManager tm;

        private ExpiryAwareX509ExtendedTrustManager(X509ExtendedTrustManager tm) {
            this.tm = tm;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkClientTrusted(chain, authType, socket);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkServerTrusted(chain, authType, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkClientTrusted(chain, authType, engine);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkServerTrusted(chain, authType, engine);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return tm.getAcceptedIssuers();
        }
    }

    private class ExpiryAwareX509TrustManager implements X509TrustManager {

        final X509TrustManager tm;

        private ExpiryAwareX509TrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            verifyExpiration(chain);
            tm.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return tm.getAcceptedIssuers();
        }
    }

    private void verifyExpiration(X509Certificate[] chain)
            throws CertificateExpiredException, CertificateNotYetValidException {
        for (X509Certificate cert : chain) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                if (policy == TrustStoreConfig.CertificateExpiryPolicy.REJECT) {
                    LOGGER.error("A certificate has expired - rejecting", e);
                    throw e;
                } else { // WARN
                    LOGGER.warn("A certificate has expired", e);
                }
            } catch (CertificateNotYetValidException e) {
                if (policy == TrustStoreConfig.CertificateExpiryPolicy.REJECT) {
                    LOGGER.error("A certificate is not yet valid - rejecting", e);
                    throw e;
                } else { // WARN
                    LOGGER.warn("A certificate is not yet valid", e);
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj != null && obj.getClass() == getClass()) {
            ExpiryTrustOptions that = (ExpiryTrustOptions) obj;
            return Objects.equals(delegate, that.delegate) &&
                    Objects.equals(policy, that.policy);
        }
        return false;
    }

    public String toString() {
        return "ExpiryTrustOptions[" +
                "delegate=" + delegate + ", " +
                "policy=" + policy + ']';
    }
}
