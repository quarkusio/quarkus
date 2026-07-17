package io.quarkus.spiffe.client.runtime.internal;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.function.Function;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

final class SpiffeTrustOptions implements TrustOptions {

    private final TrustOptions delegate;

    SpiffeTrustOptions(TrustOptions delegate) {
        this.delegate = delegate;
    }

    @Override
    public TrustOptions copy() {
        return new SpiffeTrustOptions(delegate.copy());
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
                return wrapTrustManagers(tmf.getTrustManagers());
            }
        }, tmf.getProvider(), tmf.getAlgorithm()) {
        };
    }

    @Override
    public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) {
        return Unchecked.function(s -> {
            TrustManager[] tms = delegate.trustManagerMapper(vertx).apply(s);
            return wrapTrustManagers(tms);
        });
    }

    static TrustManager[] wrapTrustManagers(TrustManager[] tms) {
        if (tms == null) {
            return null;
        }
        var wrapped = new TrustManager[tms.length];
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager x509Tm) {
                wrapped[i] = new SpiffeX509TrustManager(x509Tm);
            } else {
                throw new IllegalStateException(
                        "SPIFFE trust bundle produced a non-X509 TrustManager: " + tms[i].getClass().getName());
            }
        }
        return wrapped;
    }
}
