package io.quarkus.neo4j.deployment;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;

class NettyProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    private static final Logger log = Logger.getLogger(NettyProcessor.class);

    @BuildStep
    SubstrateConfigBuildItem build(BuildProducer<JniBuildItem> jni) {
        jni.produce(new JniBuildItem());

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                "org.neo4j.driver.internal.shaded.io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false,
                        "org.neo4j.driver.internal.shaded.io.netty.channel.socket.nio.NioServerSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));

        SubstrateConfigBuildItem.Builder builder = SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED")
                .addRuntimeInitializedClass(
                        "org.neo4j.driver.internal.shaded.io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass(
                        "org.neo4j.driver.internal.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass(
                        "org.neo4j.driver.internal.shaded.io.netty.handler.ssl.util.ThreadLocalInsecureRandom");
        try {
            Class.forName("org.neo4j.driver.internal.shaded.io.netty.handler.codec.http.HttpObjectEncoder");
            builder.addRuntimeReinitializedClass("org.neo4j.driver.internal.shaded.io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass(
                            "org.neo4j.driver.internal.shaded.io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass(
                            "org.neo4j.driver.internal.shaded.io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass(
                            "org.neo4j.driver.internal.shaded.io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");
        } catch (ClassNotFoundException e) {
            //ignore
            log.debug("Not registering Netty HTTP classes as they were not found");
        }
        return builder.build();
    }
}
