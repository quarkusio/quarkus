package io.quarkus.vertx.core.runtime.graal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.vertx.core.spi.transport.Transport;

@TargetClass(className = "io.vertx.core.impl.VertxBuilder")
final class Target_io_vertx_core_impl_VertxBuilder {
    @Substitute
    public static Transport nativeTransport() {
        return io.vertx.core.transport.Transport.NIO.implementation();
    }
}

@TargetClass(className = "io.vertx.core.net.OpenSSLEngineOptions")
final class Target_io_vertx_core_net_OpenSSLEngineOptions {

    @Substitute
    public static boolean isAvailable() {
        return false;
    }

    @Substitute
    public static boolean isAlpnAvailable() {
        return false;
    }
}

@TargetClass(className = "io.vertx.core.spi.tls.DefaultSslContextFactory")
final class Target_DefaultSslContextFactory {

    @Alias
    private Set<String> enabledCipherSuites;

    @Alias
    private List<String> applicationProtocols;

    @Alias
    private ClientAuth clientAuth;

    @Substitute
    private SslContext createContext(boolean useAlpn, boolean client, KeyManagerFactory kmf, TrustManagerFactory tmf)
            throws SSLException {
        SslContextBuilder builder;
        if (client) {
            builder = SslContextBuilder.forClient();
            if (kmf != null) {
                builder.keyManager(kmf);
            }
        } else {
            builder = SslContextBuilder.forServer(kmf);
        }
        Collection<String> cipherSuites = enabledCipherSuites;
        builder.sslProvider(SslProvider.JDK);
        if (cipherSuites == null || cipherSuites.isEmpty()) {
            cipherSuites = Target_io_vertx_core_spi_tls_DefaultJDKCipherSuite.get();
        }
        if (tmf != null) {
            builder.trustManager(tmf);
        }
        if (cipherSuites != null && cipherSuites.size() > 0) {
            builder.ciphers(cipherSuites);
        }
        if (useAlpn && applicationProtocols != null && applicationProtocols.size() > 0) {
            builder.applicationProtocolConfig(new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    applicationProtocols));
        }
        if (clientAuth != null) {
            builder.clientAuth(clientAuth);
        }
        return builder.build();
    }
}

@TargetClass(className = "io.vertx.core.spi.tls.DefaultJDKCipherSuite")
final class Target_io_vertx_core_spi_tls_DefaultJDKCipherSuite {
    @Alias
    static List<String> get() {
        return null;
    }
}

class VertxSubstitutions {

}
