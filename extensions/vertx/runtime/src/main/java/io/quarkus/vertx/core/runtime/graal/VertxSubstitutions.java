package io.quarkus.vertx.core.runtime.graal;

import java.lang.ref.Cleaner;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.vertx.core.Closeable;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.impl.CleanableWebSocketClient;
import io.vertx.core.http.impl.WebSocketClientImpl;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.WorkerPool;
import io.vertx.core.internal.net.NetClientInternal;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.impl.CleanableNetClient;
import io.vertx.core.net.impl.NetClientBuilder;
import io.vertx.core.spi.metrics.VertxMetrics;

@TargetClass(className = "io.vertx.core.impl.VertxImpl")
final class Target_io_vertx_core_impl_VertxImpl {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static Cleaner cleaner;

    @Substitute
    public Cleaner cleaner() {
        Cleaner c = cleaner;
        if (c == null) {
            synchronized (Target_io_vertx_core_impl_VertxImpl.class) {
                c = cleaner;
                if (c == null) {
                    c = Cleaner.create();
                    cleaner = c;
                }
            }
        }
        return c;
    }

    @Alias
    public VertxMetrics metrics() {
        return null;
    }

    @Alias
    private CloseFuture resolveCloseFuture() {
        return null;
    }

    @Alias
    public <C> C createSharedResource(String resourceKey, String resourceName, CloseFuture closeFuture,
            Function<CloseFuture, C> supplier) {
        return null;
    }

    @Alias
    private synchronized WorkerPool createSharedWorkerPool(CloseFuture closeFuture, String name, int poolSize,
            long maxExecuteTime, TimeUnit maxExecuteTimeUnit) {
        return null;
    }

    @Substitute
    public NetClient createNetClient(NetClientOptions options) {
        VertxInternal self = (VertxInternal) (Object) this;
        CloseFuture fut = resolveCloseFuture();
        NetClientBuilder builder = new NetClientBuilder(self, options);
        builder.metrics(metrics() != null ? metrics().createNetClientMetrics(options) : null);
        NetClientInternal netClient = builder.build();
        fut.add(netClient);
        return new CleanableNetClient(netClient, cleaner());
    }

    @Substitute
    public WebSocketClient createWebSocketClient(WebSocketClientOptions options) {
        VertxInternal self = (VertxInternal) (Object) this;
        HttpClientOptions o = new HttpClientOptions(options);
        o.setDefaultHost(options.getDefaultHost());
        o.setDefaultPort(options.getDefaultPort());
        o.setVerifyHost(options.isVerifyHost());
        o.setShared(options.isShared());
        o.setName(options.getName());
        CloseFuture cf = resolveCloseFuture();
        WebSocketClient client;
        Closeable closeable;
        if (options.isShared()) {
            CloseFuture closeFuture = new CloseFuture();
            client = createSharedResource("__vertx.shared.webSocketClients", options.getName(), closeFuture, cf_ -> {
                WebSocketClientImpl impl = new WebSocketClientImpl(self, o, options);
                cf_.add(completion -> impl.close().onComplete(completion));
                return impl;
            });
            BiFunction<Long, TimeUnit, Future<Void>> shutdown = (timeout, timeunit) -> closeFuture.close();
            client = new CleanableWebSocketClient(client, cleaner(), shutdown);
            closeable = closeFuture;
        } else {
            WebSocketClientImpl impl = new WebSocketClientImpl(self, o, options);
            closeable = impl;
            client = new CleanableWebSocketClient(impl, cleaner(), impl::shutdown);
        }
        cf.add(closeable);
        return client;
    }

    @Substitute
    public synchronized Target_io_vertx_core_impl_WorkerExecutorImpl createSharedWorkerExecutor(String name, int poolSize,
            long maxExecuteTime, TimeUnit maxExecuteTimeUnit) {
        VertxInternal self = (VertxInternal) (Object) this;
        CloseFuture execCf = new CloseFuture();
        WorkerPool sharedWorkerPool = createSharedWorkerPool(execCf, name, poolSize, maxExecuteTime, maxExecuteTimeUnit);
        CloseFuture parentCf = resolveCloseFuture();
        parentCf.add(execCf);
        return new Target_io_vertx_core_impl_WorkerExecutorImpl(self, cleaner(), sharedWorkerPool);
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

@TargetClass(className = "io.vertx.core.impl.WorkerExecutorImpl")
final class Target_io_vertx_core_impl_WorkerExecutorImpl {
    // Access the package-private constructor via @TargetClass
    @Alias
    public Target_io_vertx_core_impl_WorkerExecutorImpl(VertxInternal vertx, Cleaner cleaner, WorkerPool pool) {
    }
}

class VertxSubstitutions {

}
