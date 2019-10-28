package io.quarkus.netty.deployment;

import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.quarkus.arc.deployment.RuntimeBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;
import io.quarkus.netty.BossEventLoopGroup;
import io.quarkus.netty.MainEventLoopGroup;
import io.quarkus.netty.runtime.NettyRecorder;

class NettyProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    private static final Logger log = Logger.getLogger(NettyProcessor.class);

    static {
        InternalLoggerFactory.setDefaultFactory(new JBossNettyLoggerFactory());
    }

    @BuildStep
    public SubstrateSystemPropertyBuildItem limitMem() {
        //in native mode we limit the size of the epoll array
        //if the array overflows the selector just moves the overflow to a map
        return new SubstrateSystemPropertyBuildItem("sun.nio.ch.maxUpdateArraySize", "100");
    }

    @BuildStep
    public SystemPropertyBuildItem limitArenaSize() {
        //in native mode we limit the size of the epoll array
        //if the array overflows the selector just moves the overflow to a map
        return new SystemPropertyBuildItem("io.netty.allocator.maxOrder", "1");
    }

    @BuildStep
    SubstrateConfigBuildItem build(BuildProducer<JniBuildItem> jni) {
        boolean enableJni = false;

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioServerSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, "sun.nio.ch.SelectorImpl"));

        SubstrateConfigBuildItem.Builder builder = SubstrateConfigBuildItem.builder()
                //.addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                // Use small chunks to avoid a lot of wasted space. Default is 16mb * arenas (derived from core count)
                // Since buffers are cached to threads, the malloc overhead is temporary anyway
                .addNativeImageSystemProperty("io.netty.allocator.maxOrder", "1")
                .addRuntimeInitializedClass("io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ConscryptAlpnSslEngine")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslContext")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslClientContext")
                .addRuntimeInitializedClass("io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                .addRuntimeInitializedClass("io.netty.buffer.ByteBufUtil$HexUtil")
                .addRuntimeInitializedClass("io.netty.buffer.PooledByteBufAllocator")
                .addRuntimeInitializedClass("io.netty.buffer.ByteBufAllocator")
                .addRuntimeInitializedClass("io.netty.buffer.ByteBufUtil")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED");

        try {
            Class.forName("io.netty.handler.codec.http.HttpObjectEncoder");
            builder
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.Http2ClientUpgradeCodec")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.Http2ConnectionHandler")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.extensions.compression.DeflateDecoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");
        } catch (ClassNotFoundException e) {
            //ignore
            log.debug("Not registering Netty HTTP classes as they were not found");
        }

        try {
            Class.forName("io.netty.channel.unix.UnixChannel");
            enableJni = true;
            builder.addRuntimeInitializedClass("io.netty.channel.unix.Errors")
                    .addRuntimeInitializedClass("io.netty.channel.unix.FileDescriptor")
                    .addRuntimeInitializedClass("io.netty.channel.unix.IovArray")
                    .addRuntimeInitializedClass("io.netty.channel.unix.Limits");
        } catch (ClassNotFoundException e) {
            //ignore
            log.debug("Not registering Netty native unix classes as they were not found");
        }

        try {
            Class.forName("io.netty.channel.epoll.EpollMode");
            enableJni = true;
            builder.addRuntimeInitializedClass("io.netty.channel.epoll.Epoll")
                    .addRuntimeInitializedClass("io.netty.channel.epoll.EpollEventArray")
                    .addRuntimeInitializedClass("io.netty.channel.epoll.EpollEventLoop")
                    .addRuntimeInitializedClass("io.netty.channel.epoll.Native");
        } catch (ClassNotFoundException e) {
            //ignore
            log.debug("Not registering Netty native epoll classes as they were not found");
        }

        try {
            Class.forName("io.netty.channel.kqueue.AcceptFilter");
            enableJni = true;
            builder.addRuntimeInitializedClass("io.netty.channel.kqueue.KQueue")
                    .addRuntimeInitializedClass("io.netty.channel.kqueue.KQueueEventArray")
                    .addRuntimeInitializedClass("io.netty.channel.kqueue.KQueueEventLoop")
                    .addRuntimeInitializedClass("io.netty.channel.kqueue.Native");
        } catch (ClassNotFoundException e) {
            //ignore
            log.debug("Not registering Netty native kqueue classes as they were not found");
        }

        if (enableJni) {
            jni.produce(new JniBuildItem());
        }

        return builder //TODO: make configurable
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void eagerlyInitClass(NettyRecorder recorder) {
        //see https://github.com/quarkusio/quarkus/issues/3663
        //this class is slow to initialize, we make sure that we do it eagerly
        //before it blocks the IO thread and causes a warning
        recorder.eagerlyInitChannelId();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void createExecutors(BuildProducer<RuntimeBeanBuildItem> runtimeBeanBuildItemBuildProducer,
            Optional<EventLoopSupplierBuildItem> loopSupplierBuildItem,
            NettyRecorder recorder) {
        //TODO: configuration
        Supplier<Object> boss;
        Supplier<Object> main;
        if (loopSupplierBuildItem.isPresent()) {
            boss = (Supplier) loopSupplierBuildItem.get().getBossSupplier();
            main = (Supplier) loopSupplierBuildItem.get().getMainSupplier();
        } else {
            boss = recorder.createEventLoop(1);
            main = recorder.createEventLoop(0);
        }
        runtimeBeanBuildItemBuildProducer.produce(RuntimeBeanBuildItem.builder(EventLoopGroup.class)
                .setSupplier(boss)
                .setScope(ApplicationScoped.class)
                .addQualifier(BossEventLoopGroup.class)
                .build());
        runtimeBeanBuildItemBuildProducer.produce(RuntimeBeanBuildItem.builder(EventLoopGroup.class)
                .setSupplier(main)
                .setScope(ApplicationScoped.class)
                .addQualifier(MainEventLoopGroup.class)
                .build());
    }

    @BuildStep
    public RuntimeReinitializedClassBuildItem reinitScheduledFutureTask() {
        return new RuntimeReinitializedClassBuildItem(
                "io.quarkus.netty.runtime.graal.Holder_io_netty_util_concurrent_ScheduledFutureTask");
    }
}
