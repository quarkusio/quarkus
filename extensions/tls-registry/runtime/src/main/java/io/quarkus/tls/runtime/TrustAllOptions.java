package io.quarkus.tls.runtime;

import java.net.Socket;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;

import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

public class TrustAllOptions implements TrustOptions {

    public static TrustAllOptions INSTANCE = new TrustAllOptions();

    private static final TrustManager TRUST_ALL_MANAGER = new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }
    };

    private static final Provider PROVIDER = new Provider("", "0.0", "") {

    };

    private TrustAllOptions() {
        // Avoid direct instantiation.
    }

    @Override
    public TrustOptions copy() {
        return this;
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
        return new TrustManagerFactory(new TrustManagerFactorySpi() {
            @Override
            protected void engineInit(KeyStore keyStore) {
            }

            @Override
            protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
            }

            @Override
            protected TrustManager[] engineGetTrustManagers() {
                return new TrustManager[] { TRUST_ALL_MANAGER };
            }
        }, PROVIDER, "") {

        };
    }

    @Override
    public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) {
        return new Function<String, TrustManager[]>() {
            @Override
            public TrustManager[] apply(String name) {
                return new TrustManager[] { TRUST_ALL_MANAGER };
            }
        };
    }
}
