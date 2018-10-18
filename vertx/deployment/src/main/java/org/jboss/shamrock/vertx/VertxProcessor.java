package org.jboss.shamrock.vertx;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;

class VertxProcessor implements ResourceProcessor {

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
    	processorContext.addNativeImageSystemProperty("io.netty.noUnsafe", "true");
    	
    	processorContext.addRuntimeInitializedClasses(
    			"io.netty.handler.codec.http.HttpObjectEncoder"
    			// These may need to be added depending on the application
//    			"io.netty.handler.codec.http2.Http2CodecUtil",
//    			"io.netty.handler.codec.http2.DefaultHttp2FrameWriter",
//    			"io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder"
    			);

    	// This one may not be required after Vert.x 3.6.0 lands
    	processorContext.addReflectiveClass(false, false, "io.netty.channel.socket.nio.NioSocketChannel");
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
