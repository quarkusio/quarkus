package io.quarkus.resteasy.runtime.standalone;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.resteasy.spi.ResteasyDeployment;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.ThreadLocalHandler;
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
        if (dep != null) {
            deployment = dep;
            deployment.start();
        }
        knownPaths = known;
        contextPath = path;
    }

    public Consumer<Route> start(RuntimeValue<Vertx> vertx,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            boolean isVirtual, boolean isDefaultResourcesPath,
            Executor executor) {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                if (deployment != null) {
                    deployment.stop();
                }
            }
        });
        useDirect = !isVirtual;
        List<Handler<RoutingContext>> handlers = new ArrayList<>();

        if (hotDeploymentResourcePaths != null && !hotDeploymentResourcePaths.isEmpty()) {
            for (Path resourcePath : hotDeploymentResourcePaths) {
                String root = resourcePath.toAbsolutePath().toString();
                ThreadLocalHandler staticHandler = new ThreadLocalHandler(new Supplier<Handler<RoutingContext>>() {
                    @Override
                    public Handler<RoutingContext> get() {
                        StaticHandler staticHandler = StaticHandler.create();
                        staticHandler.setCachingEnabled(false);
                        staticHandler.setAllowRootFileSystemAccess(true);
                        staticHandler.setWebRoot(root);
                        staticHandler.setDefaultContentEncoding("UTF-8");
                        return staticHandler;
                    }
                });
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
            ThreadLocalHandler staticHandler = new ThreadLocalHandler(new Supplier<Handler<RoutingContext>>() {
                @Override
                public Handler<RoutingContext> get() {
                    return StaticHandler.create(META_INF_RESOURCES)
                            .setDefaultContentEncoding("UTF-8");
                }
            });
            handlers.add(ctx -> {
                String rel = ctx.mountPoint() == null ? ctx.normalisedPath()
                        : ctx.normalisedPath().substring(ctx.mountPoint().length());
                if (knownPaths.contains(rel)) {
                    staticHandler.handle(ctx);
                } else {
                    ctx.next();
                }
            });
        }

        if (deployment != null && isDefaultResourcesPath) {
            handlers.add(vertxRequestHandler(vertx, beanContainer, executor));
        }
        return new Consumer<Route>() {

            @Override
            public void accept(Route route) {
                for (Handler<RoutingContext> i : handlers) {
                    route.handler(i);
                }
            }
        };
    }

    public Handler<RoutingContext> vertxRequestHandler(RuntimeValue<Vertx> vertx,
            BeanContainer beanContainer, Executor executor) {
        if (deployment != null) {
            return new VertxRequestHandler(vertx.getValue(), beanContainer, deployment, contextPath, ALLOCATOR, executor);
        }
        return null;
    }

}
