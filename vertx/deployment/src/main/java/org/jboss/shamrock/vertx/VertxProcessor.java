package org.jboss.shamrock.vertx;

import javax.inject.Inject;

import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.SubstrateConfigBuildItem;

class VertxProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem build() throws Exception {


        // This one may not be required after Vert.x 3.6.0 lands
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));

        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                .build();

        // These may need to be added depending on the application
//    			"io.netty.handler.codec.http2.Http2CodecUtil",
//    			"io.netty.handler.codec.http2.DefaultHttp2FrameWriter",
//    			"io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder"
//    			);

    }

}
