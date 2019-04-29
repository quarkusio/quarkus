package io.quarkus.runtime.graal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.ssl.SslContextConfiguration;

@TargetClass(className = "javax.net.ssl.SSLContext")
public final class Target_javax_net_ssl_SSLContext {

    @Alias
    private static SSLContext defaultContext;

    @Alias
    protected Target_javax_net_ssl_SSLContext(SSLContextSpi contextSpi, Provider provider, String protocol) {
    }

    @Substitute
    public static synchronized SSLContext getDefault()
            throws NoSuchAlgorithmException {
        if (defaultContext == null) {
            if (SslContextConfiguration.isSslNativeEnabled()) {
                defaultContext = SSLContext.getInstance("Default");
            } else {
                defaultContext = new DisabledSSLContext();
            }
        }
        return defaultContext;
    }

    //    TODO sun.security.jca.GetInstance is not accessible in JDK 11. We cannot add an export
    //    as we still compile with a JDK 8 target. So for now, we will have to leave with this
    //    and only override getDefault().
    //    @Substitute
    //    public static Target_javax_net_ssl_SSLContext getInstance(String protocol)
    //            throws NoSuchAlgorithmException {
    //        Objects.requireNonNull(protocol, "null protocol name");
    //
    //        if (!SslContextConfiguration.isSslNativeEnabled()) {
    //            return (Target_javax_net_ssl_SSLContext) (Object) getDefault();
    //        }
    //
    //        GetInstance.Instance instance = GetInstance.getInstance("SSLContext", SSLContextSpi.class, protocol);
    //        return new Target_javax_net_ssl_SSLContext((SSLContextSpi) instance.impl, instance.provider,
    //                protocol);
    //    }
    //
    //    @Substitute
    //    public static Target_javax_net_ssl_SSLContext getInstance(String protocol, String provider)
    //            throws NoSuchAlgorithmException, NoSuchProviderException {
    //        Objects.requireNonNull(protocol, "null protocol name");
    //
    //        if (!SslContextConfiguration.isSslNativeEnabled()) {
    //            return (Target_javax_net_ssl_SSLContext) (Object) getDefault();
    //        }
    //
    //        GetInstance.Instance instance = GetInstance.getInstance("SSLContext", SSLContextSpi.class, protocol, provider);
    //        return new Target_javax_net_ssl_SSLContext((SSLContextSpi) instance.impl, instance.provider,
    //                protocol);
    //    }
    //
    //    @Substitute
    //    public static Target_javax_net_ssl_SSLContext getInstance(String protocol, Provider provider)
    //            throws NoSuchAlgorithmException {
    //        Objects.requireNonNull(protocol, "null protocol name");
    //
    //        if (!SslContextConfiguration.isSslNativeEnabled()) {
    //            return (Target_javax_net_ssl_SSLContext) (Object) getDefault();
    //        }
    //
    //        GetInstance.Instance instance = GetInstance.getInstance("SSLContext", SSLContextSpi.class, protocol, provider);
    //        return new Target_javax_net_ssl_SSLContext((SSLContextSpi) instance.impl, instance.provider,
    //                protocol);
    //    }

    private static class DisabledSSLContext extends SSLContext {

        protected DisabledSSLContext() {
            super(new DisabledSSLContextSpi(), new Provider("DISABLED", 1, "DISABLED") {
            }, "DISABLED");
        }
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
