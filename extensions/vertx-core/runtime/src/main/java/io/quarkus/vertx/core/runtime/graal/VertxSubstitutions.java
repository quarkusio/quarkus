package io.quarkus.vertx.core.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.eventbus.impl.HandlerHolder;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.eventbus.impl.clustered.ClusterNodeInfo;
import io.vertx.core.impl.HAManager;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.resolver.DefaultResolverProvider;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.TCPSSLOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.core.net.impl.ServerID;
import io.vertx.core.net.impl.transport.Transport;
import io.vertx.core.spi.json.JsonCodec;
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

@TargetClass(className = "io.vertx.core.eventbus.impl.clustered.ClusteredEventBus")
final class Target_io_vertx_core_eventbus_impl_clustered_ClusteredEventBusClusteredEventBus {

    @Substitute
    private NetServerOptions getServerOptions() {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    static void setCertOptions(TCPSSLOptions options, KeyCertOptions keyCertOptions) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    static void setTrustOptions(TCPSSLOptions sslOptions, TrustOptions options) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public void start(Handler<AsyncResult<Void>> resultHandler) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public void close(Handler<AsyncResult<Void>> completionHandler) {
        throw new RuntimeException("Not Implemented");

    }

    @Substitute
    public MessageImpl createMessage(boolean send, String address, MultiMap headers, Object body, String codecName,
            Handler<AsyncResult<Void>> writeHandler) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected <T> void addRegistration(boolean newAddress, String address,
            boolean replyHandler, boolean localOnly,
            Handler<AsyncResult<Void>> completionHandler) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    protected <T> void removeRegistration(HandlerHolder<T> lastHolder, String address,
            Handler<AsyncResult<Void>> completionHandler) {
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
    private void setClusterViewChangedHandler(HAManager haManager) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private int getClusterPublicPort(EventBusOptions options, int actualPort) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private String getClusterPublicHost(EventBusOptions options) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private Handler<NetSocket> getServerHandler() {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private void sendRemote(ServerID theServerID, MessageImpl message) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private void removeSub(String subName, ClusterNodeInfo node, Handler<AsyncResult<Void>> completionHandler) {
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

@TargetClass(className = "io.vertx.core.spi.json.JsonCodec", onlyWith = JacksonMissingSelector.class)
final class Target_io_vertx_core_spi_json_JsonCodec {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    static JsonCodec INSTANCE;
}

@TargetClass(className = "io.vertx.core.json.jackson.JacksonCodec", onlyWith = JacksonMissingSelector.class)
@Delete
final class Target_io_vertx_core_json_jackson_JacksonCodec {

}

@TargetClass(className = "io.vertx.core.json.Json", onlyWith = JacksonMissingSelector.class)
@Delete
final class Target_io_vertx_core_json_Json {

}

final class JacksonMissingSelector implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}

class VertxSubstitutions {

}
