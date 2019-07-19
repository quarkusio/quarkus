package io.quarkus.netty.deployment;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.deployment.RuntimeBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.netty.BossGroup;
import io.quarkus.netty.runtime.NettyRecorder;

class NettyProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    private static final Logger log = Logger.getLogger(NettyProcessor.class);

    @BuildStep
    SubstrateConfigBuildItem build() {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioServerSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));

        SubstrateConfigBuildItem.Builder builder = SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                .addRuntimeInitializedClass("io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass("io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED");
        try {
            Class.forName("io.netty.handler.codec.http.HttpObjectEncoder");
            builder.addRuntimeReinitializedClass("io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");
        } catch (ClassNotFoundException e) {
            //ignore
            log.debug("Not registering Netty HTTP classes as they were not found");
        }
        return builder //TODO: make configurable
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void createExecutors(BuildProducer<RuntimeBeanBuildItem> runtimeBeanBuildItemBuildProducer,
            NettyRecorder recorder) {
        //TODO: configuration
        Supplier<Object> boss = recorder.createEventLoop(1);
        Supplier<Object> worker = recorder.createEventLoop(0);

        runtimeBeanBuildItemBuildProducer.produce(RuntimeBeanBuildItem.builder(EventLoopGroup.class)
                .setSupplier(boss)
                .setScope(ApplicationScoped.class)
                .addQualifier(BossGroup.class)
                .build());
        runtimeBeanBuildItemBuildProducer.produce(RuntimeBeanBuildItem.builder(EventLoopGroup.class)
                .setSupplier(worker)
                .setScope(ApplicationScoped.class)
                .build());
    }

}
