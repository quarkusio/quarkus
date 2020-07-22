package io.quarkus.resteasy.runtime.standalone;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jboss.resteasy.spi.ResteasyDeployment;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Provides the runtime methods to bootstrap Resteasy in standalone mode.
 */
@Recorder
public class ResteasyStandaloneRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";

    /**
     * TODO: configuration
     */
    protected static final int BUFFER_SIZE = 8 * 1024;

    private static boolean useDirect = true;

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

    private static ResteasyDeployment deployment;
    private static String contextPath;

    public void staticInit(ResteasyDeployment dep, String path) {
        if (dep != null) {
            deployment = dep;
            deployment.start();
        }
        contextPath = path;
    }

    public void start(ShutdownContext shutdown, boolean isVirtual) {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                if (deployment != null) {
                    deployment.stop();
                }
            }
        });
        useDirect = !isVirtual;
    }

    public Handler<RoutingContext> vertxRequestHandler(Supplier<Vertx> vertx,
            BeanContainer beanContainer, Executor executor, HttpConfiguration readTimeout) {
        if (deployment != null) {
            return new VertxRequestHandler(vertx.get(), beanContainer, deployment, contextPath, ALLOCATOR, executor,
                    readTimeout.readTimeout.toMillis());
        }
        return null;
    }

}
