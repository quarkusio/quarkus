package io.quarkus.netty.runtime.graal;

import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK11OrLater;

import io.netty.bootstrap.AbstractBootstrapConfig;
import io.netty.bootstrap.ChannelFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkAlpnApplicationProtocolNegotiator;
import io.netty.handler.ssl.JdkApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

/**
 * This substitution avoid having loggers added to the build
 */
@TargetClass(className = "io.netty.util.internal.logging.InternalLoggerFactory")
final class Target_io_netty_util_internal_logging_InternalLoggerFactory {

    @Substitute
    private static InternalLoggerFactory newDefaultFactory(String name) {
        return JdkLoggerFactory.INSTANCE;
    }
}

/**
 * This substitution allows the usage of platform specific code to do low level
 * buffer related tasks
 */
@TargetClass(className = "io.netty.util.internal.CleanerJava6")
final class Target_io_netty_util_internal_CleanerJava6 {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FieldOffset, declClassName = "java.nio.DirectByteBuffer", name = "cleaner")
    private static long CLEANER_FIELD_OFFSET;
}

/**
 * This substitution allows the usage of platform specific code to do low level
 * buffer related tasks
 */
@TargetClass(className = "io.netty.util.internal.PlatformDependent0")
final class Target_io_netty_util_internal_PlatformDependent0 {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FieldOffset, declClassName = "java.nio.Buffer", name = "address")
    private static long ADDRESS_FIELD_OFFSET;
}

/**
 * This substitution allows the usage of platform specific code to do low level
 * buffer related tasks
 */
@TargetClass(className = "io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess")
final class Target_io_netty_util_internal_shaded_org_jctools_util_UnsafeRefArrayAccess {

    @Alias
    @RecomputeFieldValue(kind = Kind.ArrayIndexShift, declClass = Object[].class)
    public static int REF_ELEMENT_SHIFT;
}

// SSL
// This whole section is mostly about removing static analysis references to openssl/tcnative

@Delete
@TargetClass(className = "io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
final class Target_io_netty_handler_ssl_ReferenceCountedOpenSslEngine {
}

@TargetClass(className = "io.netty.handler.ssl.JdkSslServerContext")
final class Target_io_netty_handler_ssl_JdkSslServerContext {

    @Alias
    Target_io_netty_handler_ssl_JdkSslServerContext(Provider provider, X509Certificate[] trustCertCollection,
            TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key,
            String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers,
            CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize,
            long sessionTimeout, ClientAuth clientAuth, String[] protocols, boolean startTls)
            throws SSLException {
    }
}

@TargetClass(className = "io.netty.handler.ssl.JdkSslClientContext")
final class Target_io_netty_handler_ssl_JdkSslClientContext {

    @Alias
    Target_io_netty_handler_ssl_JdkSslClientContext(Provider sslContextProvider, X509Certificate[] trustCertCollection,
            TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key,
            String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers,
            CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, String[] protocols,
            long sessionCacheSize, long sessionTimeout)
            throws SSLException {
    }
}

@TargetClass(className = "io.netty.handler.ssl.SslHandler$SslEngineType")
final class Target_io_netty_handler_ssl_SslHandler$SslEngineType {

    @Alias
    public static Target_io_netty_handler_ssl_SslHandler$SslEngineType JDK;

    @Substitute
    static Target_io_netty_handler_ssl_SslHandler$SslEngineType forEngine(SSLEngine engine) {
        return JDK;
    }
}

@Delete
@TargetClass(className = "io.netty.handler.ssl.ConscryptAlpnSslEngine")
final class Target_io_netty_handler_ssl_ConscryptAlpnSslEngine {
}

@TargetClass(className = "io.netty.handler.ssl.JdkAlpnApplicationProtocolNegotiator$AlpnWrapper", onlyWith = JDK11OrLater.class)
final class Target_io_netty_handler_ssl_JdkAlpnApplicationProtocolNegotiator_AlpnWrapper {
    @Substitute
    public SSLEngine wrapSslEngine(SSLEngine engine, ByteBufAllocator alloc,
            JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer) {
        return (SSLEngine) (Object) new Target_io_netty_handler_ssl_Java9SslEngine(engine, applicationNegotiator, isServer);
    }

}

@TargetClass(className = "io.netty.handler.ssl.Java9SslEngine", onlyWith = JDK11OrLater.class)
final class Target_io_netty_handler_ssl_Java9SslEngine {
    @Alias
    Target_io_netty_handler_ssl_Java9SslEngine(final SSLEngine engine,
            final JdkApplicationProtocolNegotiator applicationNegotiator, final boolean isServer) {

    }
}

@TargetClass(className = "io.netty.handler.ssl.SslContext")
final class Target_io_netty_handler_ssl_SslContext {

    @Substitute
    static SslContext newServerContextInternal(SslProvider provider, Provider sslContextProvider,
            X509Certificate[] trustCertCollection,
            TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key,
            String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers,
            CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize,
            long sessionTimeout, ClientAuth clientAuth, String[] protocols, boolean startTls,
            boolean enableOcsp)
            throws SSLException {

        if (enableOcsp) {
            throw new IllegalArgumentException("OCSP is not supported with this SslProvider: " + provider);
        }
        return (SslContext) (Object) new Target_io_netty_handler_ssl_JdkSslServerContext(sslContextProvider,
                trustCertCollection,
                trustManagerFactory, keyCertChain, key, keyPassword, keyManagerFactory, ciphers, cipherFilter, apn,
                sessionCacheSize,
                sessionTimeout, clientAuth, protocols, startTls);
    }

    @Substitute
    static SslContext newClientContextInternal(SslProvider provider, Provider sslContextProvider, X509Certificate[] trustCert,
            TrustManagerFactory trustManagerFactory, X509Certificate[] keyCertChain, PrivateKey key,
            String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers,
            CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, String[] protocols,
            long sessionCacheSize, long sessionTimeout, boolean enableOcsp)
            throws SSLException {
        if (enableOcsp) {
            throw new IllegalArgumentException("OCSP is not supported with this SslProvider: " + provider);
        }
        return (SslContext) (Object) new Target_io_netty_handler_ssl_JdkSslClientContext(sslContextProvider, trustCert,
                trustManagerFactory,
                keyCertChain, key, keyPassword, keyManagerFactory, ciphers, cipherFilter, apn, protocols, sessionCacheSize,
                sessionTimeout);
    }

}

@TargetClass(className = "io.netty.handler.ssl.JdkDefaultApplicationProtocolNegotiator")
final class Target_io_netty_handler_ssl_JdkDefaultApplicationProtocolNegotiator {

    @Alias
    public static Target_io_netty_handler_ssl_JdkDefaultApplicationProtocolNegotiator INSTANCE;
}

@TargetClass(className = "io.netty.handler.ssl.JdkSslContext")
final class Target_io_netty_handler_ssl_JdkSslContext {

    @Substitute
    static JdkApplicationProtocolNegotiator toNegotiator(ApplicationProtocolConfig config, boolean isServer) {
        if (config == null) {
            return (JdkApplicationProtocolNegotiator) (Object) Target_io_netty_handler_ssl_JdkDefaultApplicationProtocolNegotiator.INSTANCE;
        }

        switch (config.protocol()) {
            case NONE:
                return (JdkApplicationProtocolNegotiator) (Object) Target_io_netty_handler_ssl_JdkDefaultApplicationProtocolNegotiator.INSTANCE;
            case ALPN:
                if (isServer) {
                    // GRAAL RC9 bug: https://github.com/oracle/graal/issues/813
                    //                switch(config.selectorFailureBehavior()) {
                    //                case FATAL_ALERT:
                    //                    return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
                    //                case NO_ADVERTISE:
                    //                    return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
                    //                default:
                    //                    throw new UnsupportedOperationException(new StringBuilder("JDK provider does not support ")
                    //                    .append(config.selectorFailureBehavior()).append(" failure behavior").toString());
                    //                }
                    SelectorFailureBehavior behavior = config.selectorFailureBehavior();
                    if (behavior == SelectorFailureBehavior.FATAL_ALERT)
                        return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
                    else if (behavior == SelectorFailureBehavior.NO_ADVERTISE)
                        return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
                    else {
                        throw new UnsupportedOperationException(new StringBuilder("JDK provider does not support ")
                                .append(config.selectorFailureBehavior()).append(" failure behavior").toString());
                    }
                } else {
                    switch (config.selectedListenerFailureBehavior()) {
                        case ACCEPT:
                            return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
                        case FATAL_ALERT:
                            return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
                        default:
                            throw new UnsupportedOperationException(new StringBuilder("JDK provider does not support ")
                                    .append(config.selectedListenerFailureBehavior()).append(" failure behavior").toString());
                    }
                }
            default:
                throw new UnsupportedOperationException(
                        new StringBuilder("JDK provider does not support ").append(config.protocol()).append(" protocol")
                                .toString());
        }
    }

}

/*
 * This one only prints exceptions otherwise we get a useless bogus
 * exception message: https://github.com/eclipse-vertx/vert.x/issues/1657
 */
@TargetClass(className = "io.netty.bootstrap.AbstractBootstrap")
final class Target_io_netty_bootstrap_AbstractBootstrap {

    @Alias
    private ChannelFactory channelFactory;

    @Alias
    void init(Channel channel) throws Exception {
    }

    @Alias
    public AbstractBootstrapConfig config() {
        return null;
    }

    @Substitute
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            channel = channelFactory.newChannel();
            init(channel);
        } catch (Throwable t) {
            // THE FIX IS HERE:
            t.printStackTrace();
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;

    }
}

class NettySubstitutions {

}
