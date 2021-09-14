package io.quarkus.vertx.core.runtime.graal;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.OpenSslServerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.eventbus.impl.HandlerHolder;
import io.vertx.core.eventbus.impl.HandlerRegistration;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.eventbus.impl.OutboundDeliveryContext;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.resolver.DefaultResolverProvider;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.impl.transport.Transport;
import io.vertx.core.spi.resolver.ResolverProvider;

@TargetClass(className = "io.vertx.core.net.impl.transport.Transport")
final class Target_io_vertx_core_net_impl_transport_Transport {
    @Substitute
    public static Transport nativeTransport() {
        return Transport.JDK;
    }
}

/**
 * This substitution forces the usage of the blocking DNS resolver
 */
@TargetClass(className = "io.vertx.core.spi.resolver.ResolverProvider")
final class TargetResolverProvider {

    @Substitute
    public static ResolverProvider factory(Vertx vertx, AddressResolverOptions options) {
        return new DefaultResolverProvider();
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

@SuppressWarnings("rawtypes")
@TargetClass(className = "io.vertx.core.eventbus.impl.clustered.ClusteredEventBus")
final class Target_io_vertx_core_eventbus_impl_clustered_ClusteredEventBusClusteredEventBus {

    @Substitute
    private NetServerOptions getServerOptions() {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public void start(Promise<Void> promise) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public void close(Promise<Void> promise) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public MessageImpl createMessage(boolean send, String address, MultiMap headers, Object body, String codecName) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected <T> void onLocalRegistration(HandlerHolder<T> handlerHolder, Promise<Void> promise) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected <T> HandlerHolder<T> createHandlerHolder(HandlerRegistration<T> registration, boolean replyHandler,
            boolean localOnly, ContextInternal context) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected <T> void onLocalUnregistration(HandlerHolder<T> handlerHolder, Promise<Void> completionHandler) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected <T> void sendOrPub(OutboundDeliveryContext<T> sendContext) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected String generateReplyAddress() {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected boolean isMessageLocal(MessageImpl msg) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    ConcurrentMap connections() {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    VertxInternal vertx() {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    EventBusOptions options() {
        throw new RuntimeException("Not Implemented");
    }
}

@TargetClass(className = "io.vertx.core.net.impl.SSLHelper")
final class Target_io_vertx_core_net_impl_SSLHelper {

    @Alias
    private boolean client;

    @Alias
    private Set<String> enabledCipherSuites;

    @Alias
    private boolean openSsl;

    @Alias
    private List<String> applicationProtocols;

    @Alias
    private KeyManagerFactory getKeyMgrFactory(VertxInternal vertx) throws Exception {
        return null;
    }

    @Substitute
    private SslContext createContext(VertxInternal vertx, boolean useAlpn, X509KeyManager mgr,
            TrustManagerFactory trustMgrFactory) {
        try {
            SslContextBuilder builder;
            if (client) {
                builder = SslContextBuilder.forClient();
                KeyManagerFactory keyMgrFactory = getKeyMgrFactory(vertx);
                if (keyMgrFactory != null) {
                    builder.keyManager(keyMgrFactory);
                }
            } else {
                if (mgr != null) {
                    builder = SslContextBuilder.forServer(mgr.getPrivateKey(null), null, mgr.getCertificateChain(null));
                } else {
                    KeyManagerFactory keyMgrFactory = getKeyMgrFactory(vertx);
                    if (keyMgrFactory == null) {
                        throw new VertxException("Key/certificate is mandatory for SSL");
                    }
                    builder = SslContextBuilder.forServer(keyMgrFactory);
                }
            }
            Collection<String> cipherSuites = enabledCipherSuites;
            if (openSsl) {
                throw new UnsupportedOperationException("OpenSSL not supported in native images");
            } else {
                builder.sslProvider(SslProvider.JDK);
                if (cipherSuites == null || cipherSuites.isEmpty()) {
                    cipherSuites = Target_io_vertx_core_net_impl_DefaultJDKCipherSuite.get();
                }
            }
            if (trustMgrFactory != null) {
                builder.trustManager(trustMgrFactory);
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
            SslContext ctx = builder.build();
            if (ctx instanceof OpenSslServerContext) {
                throw new UnsupportedOperationException("OpenSSL not supported in native images");
            }
            return ctx;
        } catch (Exception e) {
            throw new VertxException(e);
        }
    }
}

@TargetClass(className = "io.vertx.core.net.impl.DefaultJDKCipherSuite")
final class Target_io_vertx_core_net_impl_DefaultJDKCipherSuite {
    @Alias
    static List<String> get() {
        return null;
    }
}

class VertxSubstitutions {

}