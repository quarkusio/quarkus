package io.quarkus.vertx.web.common.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.netty.runtime.virtual.VirtualAddress;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.Timing;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.vertx.common.runtime.VertxCommonRecorder;
import io.quarkus.vertx.common.runtime.VertxConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.impl.VertxHandler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class VertxWebCommonRecorder {

    private static final Logger LOGGER = Logger.getLogger(VertxWebCommonRecorder.class.getName());

    private static volatile Handler<RoutingContext> hotReplacementHandler;

    private static volatile Router router;

    private static volatile Runnable closeTask;

    public static void setHotReplacement(Handler<RoutingContext> handler) {
        hotReplacementHandler = handler;
    }

    public static void shutDownDevMode() {
        closeTask.run();
        closeTask = null;
        router = null;
        hotReplacementHandler = null;
    }

    public static void startServerAfterFailedStart() {
        VertxConfiguration vertxConfiguration = new VertxConfiguration();
        ConfigInstantiator.handleObject(vertxConfiguration);
        VertxCommonRecorder.initialize(vertxConfiguration);

        try {
            HttpConfiguration config = new HttpConfiguration();
            ConfigInstantiator.handleObject(config);

            router = Router.router(VertxCommonRecorder.getVertx());
            if (hotReplacementHandler != null) {
                router.route().blockingHandler(hotReplacementHandler);
            }

            //we can't really do
            doServerStart(VertxCommonRecorder.getVertx(), config, LaunchMode.DEVELOPMENT);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeValue<Router> initializeRouter(RuntimeValue<Vertx> vertxRuntimeValue, BeanContainer container,
            HttpConfiguration httpConfiguration, LaunchMode launchMode,
            boolean startVirtual, boolean startSocket) throws IOException {

        Vertx vertx = vertxRuntimeValue.getValue();

        if (router == null) {
            router = Router.router(vertx);
            if (hotReplacementHandler != null) {
                router.route().handler(hotReplacementHandler);
            }
        }
        // Make it also possible to register the route handlers programmatically
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.select(Router.class).fire(router);

        if (startVirtual) {
            initializeVirtual(vertx);
        }
        if (startSocket) {
            // Start the server
            if (closeTask == null) {
                doServerStart(vertx, httpConfiguration, launchMode);
            }
        }

        return new RuntimeValue<>(router);
    }

    public void finalizeRouter(BeanContainer container, Handler<HttpServerRequest> defaultRouteHandler,
            List<Handler<RoutingContext>> filters, LaunchMode launchMode, ShutdownContext shutdown) {
        // install the default route at the end
        if (defaultRouteHandler != null) {
            //TODO: can we skip the router if no other routes?
            Route defaultRoute = router.route();

            for (Handler<RoutingContext> filter : filters) {
                if (filter != null) {
                    defaultRoute.handler(filter);
                }
            }

            defaultRoute.handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    defaultRouteHandler.handle(event.request());
                }
            });
        }

        if (launchMode == LaunchMode.DEVELOPMENT) {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    List<Route> routes = router.getRoutes();
                    // if the hot replacement handler has been installed, we keep it
                    for (int i = (hotReplacementHandler != null ? 1 : 0); i < routes.size(); i++) {
                        routes.get(i).remove();
                    }
                }
            });
        } else {
            shutdown.addShutdownTask(closeTask);
        }

        container.instance(RouterProducer.class).initialize(router);
    }

    private static void doServerStart(Vertx vertx, HttpConfiguration httpConfiguration, LaunchMode launchMode)
            throws IOException {
        // Http server configuration
        HttpServerOptions httpServerOptions = createHttpServerOptions(httpConfiguration, launchMode);
        HttpServerOptions sslConfig = createSslOptions(httpConfiguration, launchMode);

        int ioThreads = httpConfiguration.ioThreads.orElse(Runtime.getRuntime().availableProcessors() * 2);
        CompletableFuture<String> futureResult = new CompletableFuture<>();
        vertx.deployVerticle(new Supplier<Verticle>() {
            @Override
            public Verticle get() {
                return new WebDeploymentVerticle(httpConfiguration.determinePort(launchMode),
                        httpConfiguration.determineSslPort(launchMode), httpConfiguration.host, httpServerOptions, sslConfig,
                        router);
            }
        }, new DeploymentOptions().setInstances(ioThreads), new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if (event.failed()) {
                    futureResult.completeExceptionally(event.cause());
                } else {
                    futureResult.complete(event.result());
                }
            }
        });
        try {

            String deploymentId = futureResult.get();
            closeTask = new Runnable() {
                @Override
                public void run() {
                    CountDownLatch latch = new CountDownLatch(1);
                    vertx.undeploy(deploymentId, new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> event) {
                            latch.countDown();
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    router = null;
                    closeTask = null;
                }
            };
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to start HTTP server", e);
        }

        // TODO log proper message
        Timing.setHttpServer(String.format(
                "Listening on: http://%s:%s", httpServerOptions.getHost(), httpServerOptions.getPort()));
    }

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     *
     */
    private static HttpServerOptions createSslOptions(HttpConfiguration httpConfiguration, LaunchMode launchMode)
            throws IOException {
        ServerSslConfig sslConfig = httpConfiguration.ssl;
        //TODO: static fields break config
        final Optional<Path> certFile = sslConfig.certificate.file;
        final Optional<Path> keyFile = sslConfig.certificate.keyFile;
        final Optional<Path> keyStoreFile = sslConfig.certificate.keyStoreFile;
        final String keystorePassword = sslConfig.certificate.keyStorePassword;
        final HttpServerOptions serverOptions = new HttpServerOptions();
        if (certFile.isPresent() && keyFile.isPresent()) {
            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                    .setCertPath(certFile.get().toAbsolutePath().toString())
                    .setKeyPath(keyFile.get().toAbsolutePath().toString());
            serverOptions.setPemKeyCertOptions(pemKeyCertOptions);
        } else if (keyStoreFile.isPresent()) {
            final Path keyStorePath = keyStoreFile.get();
            final Optional<String> keyStoreFileType = sslConfig.certificate.keyStoreFileType;
            final String type;
            if (keyStoreFileType.isPresent()) {
                type = keyStoreFileType.get().toLowerCase();
            } else {
                final String pathName = keyStorePath.toString();
                if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
                    type = "pkcs12";
                } else {
                    // assume jks
                    type = "jks";
                }
            }

            //load the data
            byte[] data;
            final InputStream keystoreAsResource = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(keyStorePath.toString());

            if (keystoreAsResource != null) {
                try (InputStream is = keystoreAsResource) {
                    data = doRead(is);
                }
            } else {
                try (InputStream is = Files.newInputStream(keyStorePath)) {
                    data = doRead(is);
                }
            }

            switch (type) {
                case "pkcs12": {
                    PfxOptions options = new PfxOptions()
                            .setPassword(keystorePassword)
                            .setValue(Buffer.buffer(data));
                    serverOptions.setPfxKeyCertOptions(options);
                    break;
                }
                case "jks": {
                    JksOptions options = new JksOptions()
                            .setPassword(keystorePassword)
                            .setValue(Buffer.buffer(data));
                    serverOptions.setKeyStoreOptions(options);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown keystore type: " + type + " valid types are jks or pkcs12");
            }

        } else {
            return null;
        }

        for (String i : sslConfig.cipherSuites) {
            if (!i.isEmpty()) {
                serverOptions.addEnabledCipherSuite(i);
            }
        }

        for (String i : sslConfig.protocols) {
            if (!i.isEmpty()) {
                serverOptions.addEnabledSecureTransportProtocol(i);
            }
        }
        serverOptions.setSsl(true);
        serverOptions.setHost(httpConfiguration.host);
        serverOptions.setPort(httpConfiguration.determineSslPort(launchMode));
        return serverOptions;
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    private static HttpServerOptions createHttpServerOptions(HttpConfiguration httpConfiguration, LaunchMode launchMode) {
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(httpConfiguration.host);
        options.setPort(httpConfiguration.determinePort(launchMode));
        return options;
    }

    public void warnIfPortChanged(HttpConfiguration config, int port) {
        if (config.port != port) {
            LOGGER.errorf(
                    "quarkus.http.port was specified at build time as %s however run time value is %s, Kubernetes metadata will be incorrect.",
                    port, config.port);
        }
    }

    private static class WebDeploymentVerticle implements Verticle {

        private final int port;
        private final int httpsPort;
        private final String host;
        private HttpServer httpServer;
        private HttpServer httpsServer;
        private Vertx vertx;
        private final HttpServerOptions httpOptions;
        private final HttpServerOptions httpsOptions;
        private final Router router;

        public WebDeploymentVerticle(int port, int httpsPort, String host, HttpServerOptions httpOptions,
                HttpServerOptions httpsOptions, Router router) {
            this.port = port;
            this.httpsPort = httpsPort;
            this.host = host;
            this.httpOptions = httpOptions;
            this.httpsOptions = httpsOptions;
            this.router = router;
        }

        @Override
        public Vertx getVertx() {
            return vertx;
        }

        @Override
        public void init(Vertx vertx, Context context) {
            this.vertx = vertx;
        }

        @Override
        public void start(Future<Void> startFuture) throws Exception {
            final AtomicInteger remainingCount = new AtomicInteger(httpsOptions != null ? 2 : 1);
            Handler<AsyncResult<HttpServer>> doneHandler = event -> {
                if (remainingCount.decrementAndGet() == 0) {
                    startFuture.complete();
                }
            };
            httpServer = vertx.createHttpServer(httpOptions);
            httpServer.requestHandler(router);
            httpServer.listen(port, host, doneHandler);
            if (httpsOptions != null) {
                httpsServer = vertx.createHttpServer(httpsOptions);
                httpsServer.requestHandler(router);
                httpsServer.listen(httpsPort, host, doneHandler);
            }
        }

        @Override
        public void stop(Future<Void> stopFuture) throws Exception {
            httpServer.close(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    if (httpsServer != null) {
                        httpsServer.close(new Handler<AsyncResult<Void>>() {
                            @Override
                            public void handle(AsyncResult<Void> event) {
                                stopFuture.complete();
                            }
                        });
                    } else {
                        stopFuture.complete();
                    }
                }
            });
        }
    }

    protected static ServerBootstrap virtualBootstrap;
    public static VirtualAddress VIRTUAL_HTTP = new VirtualAddress("netty-virtual-http");

    private static void initializeVirtual(Vertx vertxRuntime) {
        if (virtualBootstrap != null)
            return;
        VertxInternal vertx = (VertxInternal) vertxRuntime;
        virtualBootstrap = new ServerBootstrap();

        virtualBootstrap.group(vertx.getEventLoopGroup())
                .channel(VirtualServerChannel.class)
                .handler(new ChannelInitializer<VirtualServerChannel>() {
                    @Override
                    public void initChannel(VirtualServerChannel ch) throws Exception {
                        //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                    }
                })
                .childHandler(new ChannelInitializer<VirtualChannel>() {
                    @Override
                    public void initChannel(VirtualChannel ch) throws Exception {
                        ContextInternal context = (ContextInternal) vertx.createEventLoopContext(null, null, new JsonObject(),
                                Thread.currentThread().getContextClassLoader());
                        VertxHandler<Http1xServerConnection> handler = VertxHandler.create(context, chctx -> {
                            Http1xServerConnection conn = new Http1xServerConnection(
                                    context.owner(),
                                    null,
                                    new HttpServerOptions(),
                                    chctx,
                                    context,
                                    "localhost",
                                    null);
                            conn.handler(router);
                            return conn;
                        });

                        ch.pipeline().addLast("handler", handler);
                    }
                });

        // Start the server.
        try {
            virtualBootstrap.bind(VIRTUAL_HTTP).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to bind virtual http");
        }

    }

    public static Router getRouter() {
        return router;
    }
}
