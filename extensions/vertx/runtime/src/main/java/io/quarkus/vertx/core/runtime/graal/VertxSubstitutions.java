package io.quarkus.vertx.core.runtime.graal;

import static io.quarkus.vertx.core.runtime.graal.VertxSubstitutions.HTTP3_QUIC_NOT_AVAILABLE_MESSAGE;

import java.lang.ref.Cleaner;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.channel.Channel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.quarkus.netty.runtime.graal.TcnativeAbsent;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.WorkerPool;

/**
 * Hard-code the OpenSSL engine as unavailable, only when netty-tcnative is not on the classpath. When it is, the
 * original Vert.x code runs and the probe reflects the real state of the native library.
 */
@TargetClass(className = "io.vertx.core.net.OpenSSLEngineOptions", onlyWith = TcnativeAbsent.class)
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

/**
 * Force the JDK provider in the default SslContextFactory, only when netty-tcnative is not on the classpath; with
 * tcnative present the original implementation picks the provider from the engine options.
 */
@TargetClass(className = "io.vertx.core.spi.tls.DefaultSslContextFactory", onlyWith = TcnativeAbsent.class)
final class Target_DefaultSslContextFactory {

    @Alias
    private Set<String> enabledCipherSuites;

    @Alias
    private List<String> applicationProtocols;

    @Alias
    private ClientAuth clientAuth;

    @Alias
    private SNIServerName serverName;

    @Alias
    private String endpointIdentificationAlgorithm;

    @Alias
    private Set<String> enabledProtocols;

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
                    ApplicationProtocolConfig.SelectorFailureBehavior.FATAL_ALERT,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.FATAL_ALERT,
                    applicationProtocols));
        }
        if (client) {
            if (serverName != null) {
                builder.serverName(serverName);
            }
            builder.endpointIdentificationAlgorithm(
                    endpointIdentificationAlgorithm == null ? "" : endpointIdentificationAlgorithm);
        } else {
            if (clientAuth != null) {
                builder.clientAuth(clientAuth);
            }
        }
        if (enabledProtocols != null) {
            builder.protocols(enabledProtocols);
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

@TargetClass(className = "io.vertx.core.impl.WorkerExecutorImpl")
final class Target_io_vertx_core_impl_WorkerExecutorImpl {
    // Access the package-private constructor via @TargetClass
    @Alias
    public Target_io_vertx_core_impl_WorkerExecutorImpl(VertxInternal vertx, Cleaner cleaner, WorkerPool pool) {
    }
}

/*
 * Vert.x core substitutions that cut QUIC reachability from always-reachable Vert.x classes.
 * The Netty-level Quiche substitutions are in the netty extension NettySubstitutions class
 * (to have the same JPMS module as the target classes).
 */

@TargetClass(className = "io.vertx.core.net.impl.ConnectionBase", onlyWith = IsQuarkusHttp3Absent.class)
final class Target_io_vertx_core_net_impl_ConnectionBase {

    @Alias
    Channel channel;

    @Alias
    VertxInternal vertx;

    @Substitute
    io.vertx.core.net.SocketAddress channelRemoteAddress() {
        java.net.SocketAddress addr = channel.remoteAddress();
        return addr != null ? vertx.transport().convert(addr) : null;
    }

    @Substitute
    io.vertx.core.net.SocketAddress channelLocalAddress() {
        java.net.SocketAddress addr = channel.localAddress();
        return addr != null ? vertx.transport().convert(addr) : null;
    }
}

@TargetClass(className = "io.vertx.core.http.impl.HybridHttpServer", onlyWith = IsQuarkusHttp3Absent.class)
final class Target_io_vertx_core_http_impl_HybridHttpServer {

    @Substitute
    public io.vertx.core.internal.http.HttpServerInternal quicServer(
            io.vertx.core.spi.metrics.HttpServerMetrics<?, ?> httpMetrics) {
        throw new UnsupportedOperationException(HTTP3_QUIC_NOT_AVAILABLE_MESSAGE);
    }
}

@TargetClass(className = "io.vertx.core.net.impl.quic.QuicServerImpl", onlyWith = IsQuarkusHttp3Absent.class)
final class Target_io_vertx_core_net_impl_quic_QuicServerImpl {

    @Substitute
    public static io.vertx.core.net.QuicServer create(VertxInternal vertx,
            io.vertx.core.net.QuicServerConfig config,
            io.vertx.core.net.ServerSSLOptions sslOptions) {
        throw new UnsupportedOperationException(HTTP3_QUIC_NOT_AVAILABLE_MESSAGE);
    }
}

@TargetClass(className = "io.vertx.core.net.impl.quic.QuicClientImpl", onlyWith = IsQuarkusHttp3Absent.class)
final class Target_io_vertx_core_net_impl_quic_QuicClientImpl {

    @Substitute
    public static io.vertx.core.net.QuicClient create(VertxInternal vertx,
            io.vertx.core.net.QuicClientConfig config,
            io.vertx.core.net.ClientSSLOptions sslOptions) {
        throw new UnsupportedOperationException(HTTP3_QUIC_NOT_AVAILABLE_MESSAGE);
    }
}

@TargetClass(className = "io.vertx.core.http.impl.quic.QuicHttpServer", onlyWith = IsQuarkusHttp3Absent.class)
final class Target_io_vertx_core_http_impl_quic_QuicHttpServer {

    @Substitute
    Target_io_vertx_core_http_impl_quic_QuicHttpServer(
            VertxInternal vertx,
            io.vertx.core.http.HttpServerConfig config,
            io.vertx.core.net.ServerSSLOptions sslOptions,
            io.vertx.core.spi.metrics.HttpServerMetrics<?, ?> httpMetrics) {
        throw new UnsupportedOperationException(HTTP3_QUIC_NOT_AVAILABLE_MESSAGE);
    }
}

@TargetClass(className = "io.vertx.core.http.impl.quic.QuicHttpClientTransport", onlyWith = IsQuarkusHttp3Absent.class)
final class Target_io_vertx_core_http_impl_quic_QuicHttpClientTransport {

    @Substitute
    Target_io_vertx_core_http_impl_quic_QuicHttpClientTransport(
            VertxInternal vertx,
            io.vertx.core.http.HttpClientConfig config) {
        throw new UnsupportedOperationException(HTTP3_QUIC_NOT_AVAILABLE_MESSAGE);
    }
}

class IsQuarkusHttp3Absent implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("io.quarkus.http3.runtime.Http3Recorder");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}

class VertxSubstitutions {
    public static final String HTTP3_QUIC_NOT_AVAILABLE_MESSAGE = "HTTP/3 (QUIC) is not available - add the quarkus-http3 extension";
}
