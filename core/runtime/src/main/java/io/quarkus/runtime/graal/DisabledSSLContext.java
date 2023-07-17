package io.quarkus.runtime.graal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class DisabledSSLContext extends SSLContext {

    public DisabledSSLContext() {
        super(new DisabledSSLContextSpi(), new Provider("DISABLED", "1.0", "DISABLED") {
        }, "DISABLED");
    }

    private static class DisabledSSLContextSpi extends SSLContextSpi {

        @Override
        protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom)
                throws KeyManagementException {
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return new SSLSocketFactory() {
                @Override
                public String[] getDefaultCipherSuites() {
                    return new String[0];
                }

                @Override
                public String[] getSupportedCipherSuites() {
                    return new String[0];
                }

                @Override
                public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
                    throw sslSupportDisabledException();
                }

                @Override
                public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
                    throw sslSupportDisabledException();
                }

                @Override
                public Socket createSocket(String s, int i, InetAddress inetAddress, int i1)
                        throws IOException, UnknownHostException {
                    throw sslSupportDisabledException();
                }

                @Override
                public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
                    throw sslSupportDisabledException();
                }

                @Override
                public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1)
                        throws IOException {
                    throw sslSupportDisabledException();
                }
            };
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            throw sslSupportDisabledException();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            throw sslSupportDisabledException();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String s, int i) {
            throw sslSupportDisabledException();
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            throw sslSupportDisabledException();
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            throw sslSupportDisabledException();
        }

        private RuntimeException sslSupportDisabledException() {
            return new IllegalStateException(
                    "Native SSL support is disabled: you have set quarkus.ssl.native to false in your configuration.");
        }
    }
}
