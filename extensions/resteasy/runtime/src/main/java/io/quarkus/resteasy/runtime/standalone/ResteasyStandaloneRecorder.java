package io.quarkus.resteasy.runtime.standalone;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
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
    private static Set<String> knownPaths;
    private static String contextPath;

    public void staticInit(ResteasyDeployment dep, String path, Set<String> known) {
        deployment = dep;
        deployment.start();
        knownPaths = known;
        contextPath = path;
    }

    public Consumer<Route> start(RuntimeValue<Vertx> vertxValue,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            boolean isVirtual) {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                deployment.stop();
            }
        });
        Vertx vertx = vertxValue.getValue();
        useDirect = !isVirtual;
        List<Handler<RoutingContext>> handlers = new ArrayList<>();

        if (hotDeploymentResourcePaths != null && !hotDeploymentResourcePaths.isEmpty()) {
            for (Path resourcePath : hotDeploymentResourcePaths) {
                String root = resourcePath.toAbsolutePath().toString();
                StaticHandler staticHandler = StaticHandler.create();
                staticHandler.setCachingEnabled(false);
                staticHandler.setAllowRootFileSystemAccess(true);
                staticHandler.setWebRoot(root);
                handlers.add(event -> {
                    try {
                        staticHandler.handle(event);
                    } catch (Exception e) {
                        // on Windows, the drive in file path screws up cache lookup
                        // so just punt to next handler
                        event.next();
                    }
                });
            }
        }
        if (!knownPaths.isEmpty()) {
            StaticHandler staticHandler = StaticHandler.create(META_INF_RESOURCES);
            handlers.add(ctx -> {
                if (knownPaths.contains(ctx.normalisedPath())) {
                    staticHandler.handle(ctx);
                } else {
                    ctx.next();
                }
            });
        }

        VertxRequestHandler requestHandler = new VertxRequestHandler(vertx, beanContainer, deployment, contextPath, ALLOCATOR);

        handlers.add(requestHandler);
        return new Consumer<Route>() {

            @Override
            public void accept(Route route) {
                for (Handler<RoutingContext> i : handlers) {
                    route.handler(i);
                }
            }
        };
    }

}
