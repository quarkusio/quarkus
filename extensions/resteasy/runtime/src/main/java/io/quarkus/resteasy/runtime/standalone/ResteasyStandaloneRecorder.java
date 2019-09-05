package io.quarkus.resteasy.runtime.standalone;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyDeployment;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;

/**
 * Provides the runtime methods to bootstrap Resteasy in standalone mode.
 */
@Recorder
public class ResteasyStandaloneRecorder {

    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    /**
     * TODO: configuration
     */
    protected static final int BUFFER_SIZE = 8 * 1024;

    private static boolean useDirect = true;

    private static Handler<HttpServerRequest> ROOT_HANDLER = new Handler<HttpServerRequest>() {
        @Override
        public void handle(HttpServerRequest httpServerRequest) {
            currentRoot.handle(httpServerRequest);
        }
    };

    private static VertxRequestHandler currentRoot = null;

    //TODO: clean this up
    private static BufferAllocator ALLOCATOR = new BufferAllocator() {
        @Override
        public ByteBuf allocateBuffer() {
            return allocateBuffer(useDirect);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct) {
            return allocateBuffer(direct, BUFFER_SIZE);
        }

        @Override
        public ByteBuf allocateBuffer(int bufferSize) {
            return allocateBuffer(useDirect, bufferSize);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct, int bufferSize) {
            if (direct) {
                return PooledByteBufAllocator.DEFAULT.directBuffer(bufferSize);
            } else {
                return PooledByteBufAllocator.DEFAULT.heapBuffer(bufferSize);
            }
        }

        @Override
        public int getBufferSize() {
            return BUFFER_SIZE;
        }
    };

    public Handler<HttpServerRequest> startResteasy(RuntimeValue<Vertx> vertxValue,
            String contextPath,
            ResteasyDeployment deployment,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            boolean isVirtual) throws Exception {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                deployment.stop();
            }
        });
        Vertx vertx = vertxValue.getValue();
        deployment.start();
        useDirect = !isVirtual;

        currentRoot = new VertxRequestHandler(vertx, beanContainer, deployment, contextPath, ALLOCATOR);
        return ROOT_HANDLER;
    }

}
