package io.quarkus.resteasy.runtime.standalone;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyDeployment;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.quarkus.resteasy.runtime.ResteasyVertxConfig;
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

    private static boolean useDirect = true;

    private static ResteasyDeployment deployment;
    private static String contextPath;

    public void staticInit(ResteasyDeployment dep, String path) {
        if (dep != null) {
            deployment = dep;
            deployment.getDefaultContextObjects().put(ResteasyConfiguration.class, new ResteasyConfigurationMPConfig());
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

    public Handler<RoutingContext> vertxRequestHandler(Supplier<Vertx> vertx, Executor executor, HttpConfiguration readTimeout,
            ResteasyVertxConfig config) {
        if (deployment != null) {
            return new VertxRequestHandler(vertx.get(), deployment, contextPath,
                    new ResteasyVertxAllocator(config.responseBufferSize), executor,
                    readTimeout.readTimeout.toMillis());
        }
        return null;
    }

    private static class ResteasyVertxAllocator implements BufferAllocator {

        private final int bufferSize;

        private ResteasyVertxAllocator(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public ByteBuf allocateBuffer() {
            return allocateBuffer(useDirect);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct) {
            return allocateBuffer(direct, bufferSize);
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
            return bufferSize;
        }
    }
}
