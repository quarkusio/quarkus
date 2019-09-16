package io.quarkus.resteasy.runtime.standalone;

import java.nio.file.Path;
import java.util.List;

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
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Provides the runtime methods to bootstrap Resteasy in standalone mode.
 */
@Recorder
public class ResteasyStandaloneRecorder {

    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");
    public static final String META_INF_RESOURCES = "META-INF/resources";

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

    private static Handler<HttpServerRequest> currentRoot = null;

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

    private static volatile List<Path> hotDeploymentResourcePaths;

    public static void setHotDeploymentResources(List<Path> resources) {
        hotDeploymentResourcePaths = resources;
    }

    private static ResteasyDeployment deployment;

    public void setupDeployment(ResteasyDeployment dep) {
        deployment = dep;
        deployment.start();

    }

    public Handler<HttpServerRequest> startResteasy(RuntimeValue<Vertx> vertxValue,
            String contextPath,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            boolean hasClasspathResources,
            boolean isVirtual) throws Exception {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                deployment.stop();
            }
        });
        Vertx vertx = vertxValue.getValue();
        useDirect = !isVirtual;

        Router router = null;
        if (hotDeploymentResourcePaths != null && !hotDeploymentResourcePaths.isEmpty()) {
            router = router == null ? Router.router(vertx) : router;
            for (Path resourcePath : hotDeploymentResourcePaths) {
                String root = resourcePath.toAbsolutePath().toString();
                StaticHandler staticHandler = StaticHandler.create();
                staticHandler.setCachingEnabled(false);
                staticHandler.setAllowRootFileSystemAccess(true);
                staticHandler.setWebRoot(root);
                router.route().handler(staticHandler);
            }
        }
        if (hasClasspathResources) {
            router = router == null ? Router.router(vertx) : router;
            router.route().handler(StaticHandler.create(META_INF_RESOURCES));
        }

        VertxRequestHandler requestHandler = new VertxRequestHandler(vertx, beanContainer, deployment, contextPath, ALLOCATOR);

        // We don't to add a Router if we don't have to
        if (router == null) {
            currentRoot = requestHandler;
        } else {
            router.route().handler(event -> requestHandler.handle(event.request()));
            currentRoot = router;
        }

        return ROOT_HANDLER;
    }

}
