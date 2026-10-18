package io.quarkus.runtime.graal;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;

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
}
