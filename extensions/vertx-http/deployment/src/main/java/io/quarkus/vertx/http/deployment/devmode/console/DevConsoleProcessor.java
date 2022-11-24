package io.quarkus.vertx.http.deployment.devmode.console;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.WebSocketLogHandlerBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.deployment.ide.EffectiveIdeBuildItem;
import io.quarkus.deployment.ide.Ide;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleWebjarBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.HtmlEscaper;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.RawString;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.ResultMapper;
import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.TemplateNode.Origin;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.ValueResolvers;
import io.quarkus.qute.Variant;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.utilities.OS;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleCORSFilter;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleFilter;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleRecorder;
import io.quarkus.vertx.http.runtime.devmode.RedirectHandler;
import io.quarkus.vertx.http.runtime.devmode.RuntimeDevConsoleRoute;
import io.quarkus.vertx.http.runtime.logstream.LogStreamRecorder;
import io.quarkus.vertx.http.runtime.logstream.WebSocketLogHandler;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.config.common.utils.StringUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.VertxThread;
import io.vertx.core.net.impl.VertxHandler;
import io.vertx.core.net.impl.transport.Transport;
import io.vertx.core.spi.VertxThreadFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class DevConsoleProcessor {

    private static final Logger log = Logger.getLogger(DevConsoleProcessor.class);

    private static final GACT DEVCONSOLE_WEBJAR_ARTIFACT_KEY = new GACT("io.quarkus", "quarkus-vertx-http-deployment", null,
            "jar");
    private static final String DEVCONSOLE_WEBJAR_STATIC_RESOURCES_PATH = "dev-static/";
    private static final Object EMPTY = new Object();

    // FIXME: config, take from Qute?
    private static final String[] suffixes = new String[] { "html", "txt" };
    protected static volatile ServerBootstrap virtualBootstrap;
    protected static volatile Vertx devConsoleVertx;
    protected static volatile Channel channel;
    static Router router;
    static Router mainRouter;

    public static void initializeVirtual() {
        if (virtualBootstrap != null) {
            return;
        }
        devConsoleVertx = initializeDevConsoleVertx();
        VertxInternal vertx = (VertxInternal) devConsoleVertx;
        QuarkusClassLoader ccl = (QuarkusClassLoader) DevConsoleProcessor.class.getClassLoader();
        ccl.addCloseTask(new Runnable() {
            @Override
            public void run() {
                virtualBootstrap = null;
                if (channel != null) {
                    try {
                        channel.close().sync();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("failed to close virtual http");
                    }
                }
                if (devConsoleVertx != null) {
                    devConsoleVertx.close();
                    devConsoleVertx = null;
                }
            }
        });
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
                        // Vert.x 4 Migration: Verify this behavior
                        EventLoopContext context = vertx.createEventLoopContext();

                        VertxHandler<Http1xServerConnection> handler = VertxHandler.create(chctx -> {
                            Http1xServerConnection connection = new Http1xServerConnection(
                                    () -> (ContextInternal) VertxContext.getOrCreateDuplicatedContext(context),
                                    null,
                                    new HttpServerOptions(),
                                    chctx,
                                    context,
                                    "localhost",
                                    null);
                            connection.handler(new Handler<HttpServerRequest>() {
                                @Override
                                public void handle(HttpServerRequest event) {
                                    mainRouter.handle(event);
                                }
                            });
                            return connection;
                        });
                        ch.pipeline().addLast("handler", handler);
                    }
                });

        // Start the server.
        try {
            ChannelFuture future = virtualBootstrap.bind(DevConsoleHttpHandler.QUARKUS_DEV_CONSOLE);
            future.sync();
            channel = future.channel();
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to bind virtual http");
        }

    }

    /**
     * Boots the Vert.x instance used by the DevConsole,
     * applying some minimal tuning and customizations.
     *
     * @return the initialized Vert.x instance
     */
    private static Vertx initializeDevConsoleVertx() {
        final VertxOptions vertxOptions = new VertxOptions();
        //Smaller than default, but larger than 1 to be on the safe side.
        int POOL_SIZE = 2;
        vertxOptions.setEventLoopPoolSize(POOL_SIZE);
        vertxOptions.setWorkerPoolSize(POOL_SIZE);
        vertxOptions.getMetricsOptions().setEnabled(false);
        //Not good for development:
        vertxOptions.getFileSystemOptions().setFileCachingEnabled(false);
        vertxOptions.getFileSystemOptions().setClassPathResolvingEnabled(false);
        VertxBuilder builder = new VertxBuilder(vertxOptions);
        builder.threadFactory(new VertxThreadFactory() {
            @Override
            public VertxThread newVertxThread(Runnable target, String name, boolean worker, long maxExecTime,
                    TimeUnit maxExecTimeUnit) {
                //Customize the Thread names so to not conflict with the names generated by the main Quarkus Vert.x instance
                return new VertxThread(target, "[DevConsole]" + name, worker, maxExecTime, maxExecTimeUnit);
            }
        });
        builder.transport(Transport.JDK);
        builder.init();
        return builder.vertx();
    }

    protected static void newRouter(Engine engine,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        // "/" or "/myroot/"
        String httpRootPath = nonApplicationRootPathBuildItem.getNormalizedHttpRootPath();
        // "/" or "/myroot/" or "/q/" or "/myroot/q/"
        String frameworkRootPath = nonApplicationRootPathBuildItem.getNonApplicationRootPath();

        Handler<RoutingContext> errorHandler = new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                String message = "Dev console request failed";
                log.error(message, event.failure());
                event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
                event.response().end(
                        new TemplateHtmlBuilder("Internal Server Error", message, message).stack(event.failure()).toString());
            }
        };
        router = Router.router(devConsoleVertx);
        router.errorHandler(500, errorHandler);

        router.route()
                .order(Integer.MIN_VALUE)
                .handler(new FlashScopeHandler());

        router.route().method(HttpMethod.GET)
                .order(Integer.MIN_VALUE + 1)
                .handler(new DevConsole(engine, httpRootPath, frameworkRootPath));
        mainRouter = Router.router(devConsoleVertx);
        mainRouter.errorHandler(500, errorHandler);
        mainRouter.route(nonApplicationRootPathBuildItem.resolvePath("dev*")).subRouter(router);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public ServiceStartBuildItem buildTimeTemplates(List<DevConsoleTemplateInfoBuildItem> items,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        Map<String, Map<String, Object>> results = new HashMap<>();
        for (DevConsoleTemplateInfoBuildItem i : items) {
            Entry<String, String> groupAndArtifact = i.groupIdAndArtifactId(curateOutcomeBuildItem);
            Map<String, Object> map = results.computeIfAbsent(groupAndArtifact.getKey() + "." + groupAndArtifact.getValue(),
                    (s) -> new HashMap<>());
            map.put(i.getName(), i.getObject());
        }
        DevConsoleManager.setTemplateInfo(results);
        return null;
    }

    @BuildStep
    DevTemplateVariantsBuildItem collectTemplateVariants(List<DevTemplatePathBuildItem> templatePaths) throws IOException {
        Set<String> allPaths = templatePaths.stream().map(DevTemplatePathBuildItem::getPath).collect(Collectors.toSet());
        // item -> [item.html, item.txt]
        // ItemResource/item -> -> [ItemResource/item.html, ItemResource/item.xml]
        Map<String, List<String>> baseToVariants = new HashMap<>();
        for (String path : allPaths) {
            int idx = path.lastIndexOf('.');
            if (idx != -1) {
                String base = path.substring(0, idx);
                List<String> variants = baseToVariants.get(base);
                if (variants == null) {
                    variants = new ArrayList<>();
                    baseToVariants.put(base, variants);
                }
                variants.add(path);
            }
        }
        return new DevTemplateVariantsBuildItem(baseToVariants);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeTemplates(List<DevConsoleRuntimeTemplateInfoBuildItem> items, DevConsoleRecorder recorder,
            List<ServiceStartBuildItem> gate) {
        for (DevConsoleRuntimeTemplateInfoBuildItem i : items) {
            recorder.addInfo(i.getGroupId(), i.getArtifactId(), i.getName(), i.getObject());
        }
        recorder.initConfigFun();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void handler(BuildProducer<HistoryHandlerBuildItem> historyProducer,
            BuildProducer<WebSocketLogHandlerBuildItem> webSocketLogHandlerBuildItem,
            LogStreamRecorder recorder, DevUIConfig devUiConfig) {
        RuntimeValue<Optional<WebSocketLogHandler>> handler = recorder.logHandler(devUiConfig.historySize);

        webSocketLogHandlerBuildItem.produce(new WebSocketLogHandlerBuildItem((RuntimeValue) handler));
        historyProducer.produce(new HistoryHandlerBuildItem(handler));
    }

    @Consume(LoggingSetupBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    public ServiceStartBuildItem setupDeploymentSideHandling(List<DevTemplatePathBuildItem> devTemplatePaths,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem,
            List<RouteBuildItem> allRoutes,
            List<DevConsoleRouteBuildItem> routes,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return null;
        }

        initializeVirtual();
        Engine quteEngine = buildEngine(devTemplatePaths,
                allRoutes,
                buildSystemTargetBuildItem,
                effectiveIdeBuildItem,
                nonApplicationRootPathBuildItem,
                configDescriptionBuildItems,
                launchModeBuildItem);
        newRouter(quteEngine, nonApplicationRootPathBuildItem);

        for (DevConsoleRouteBuildItem i : routes) {
            Entry<String, String> groupAndArtifact = i.groupIdAndArtifactId(curateOutcomeBuildItem);
            // deployment side handling
            if (i.isDeploymentSide()) {
                Route route = router
                        .route("/" + groupAndArtifact.getKey() + "." + groupAndArtifact.getValue() + "/" + i.getPath());
                if (i.getMethod() != null) {
                    route = route.method(HttpMethod.valueOf(i.getMethod()));
                }
                if (i.isBodyHandlerRequired()) {
                    route.handler(BodyHandler.create());
                }
                if (i.isBlockingHandler()) {
                    route.blockingHandler(i.getHandler());
                } else {
                    route.handler(i.getHandler());
                }
            }
        }

        return null;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public WebJarBuildItem setupWebJar(
            LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return null;
        }
        return WebJarBuildItem.builder().artifactKey(DEVCONSOLE_WEBJAR_ARTIFACT_KEY) //
                .root(DEVCONSOLE_WEBJAR_STATIC_RESOURCES_PATH) //
                .build();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void setupDevConsoleWebjar(
            List<DevConsoleWebjarBuildItem> devConsoleWebjarBuildItems,
            BuildProducer<WebJarBuildItem> webJarBuildItemBuildProducer,
            LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }
        for (DevConsoleWebjarBuildItem devConsoleWebjar : devConsoleWebjarBuildItems) {
            webJarBuildItemBuildProducer.produce(WebJarBuildItem.builder()
                    .artifactKey(devConsoleWebjar.getArtifactKey())
                    .root(devConsoleWebjar.getRoot())
                    .useDefaultQuarkusBranding(devConsoleWebjar.getUseDefaultQuarkusBranding())
                    .onlyCopyNonArtifactFiles(devConsoleWebjar.getOnlyCopyNonArtifactFiles())
                    .build());
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    public void setupDevConsoleRoutes(
            List<DevConsoleWebjarBuildItem> devConsoleWebjarBuildItems,
            DevConsoleRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ShutdownContextBuildItem shutdownContext,
            BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            WebJarResultsBuildItem webJarResultsBuildItem) {

        for (DevConsoleWebjarBuildItem webjarBuildItem : devConsoleWebjarBuildItems) {
            WebJarResultsBuildItem.WebJarResult result = webJarResultsBuildItem.byArtifactKey(webjarBuildItem.getArtifactKey());
            if (result == null) {
                continue;
            }
            routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route("dev/" + webjarBuildItem.getRouteRoot() + "/*")
                    .handler(recorder.fileSystemStaticHandler(result.getWebRootConfigurations(), shutdownContext))
                    .build());
        }
    }

    @BuildStep(onlyIf = { IsDevelopment.class })
    public DevConsoleTemplateInfoBuildItem config(List<DevServiceDescriptionBuildItem> serviceDescriptions) {
        return new DevConsoleTemplateInfoBuildItem("devServices", serviceDescriptions);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(LoggingSetupBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    public void setupDevConsoleRoutes(
            DevUIConfig devUIConfig,
            DevConsoleRecorder recorder,
            LogStreamRecorder logStreamRecorder,
            List<DevConsoleRouteBuildItem> routes,
            HistoryHandlerBuildItem historyHandlerBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ShutdownContextBuildItem shutdownContext,
            BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            WebJarResultsBuildItem webJarResultsBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BodyHandlerBuildItem bodyHandlerBuildItem) {

        WebJarResultsBuildItem.WebJarResult result = webJarResultsBuildItem.byArtifactKey(DEVCONSOLE_WEBJAR_ARTIFACT_KEY);

        if (result == null) {
            return;
        }

        routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route("dev/resources/*")
                .handler(recorder.fileSystemStaticHandler(
                        result.getWebRootConfigurations(), shutdownContext))
                .build());

        // Add the log stream
        routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route("dev/logstream")
                .handler(logStreamRecorder.websocketHandler(historyHandlerBuildItem.value))
                .build());

        for (DevConsoleRouteBuildItem i : routes) {
            Entry<String, String> groupAndArtifact = i.groupIdAndArtifactId(curateOutcomeBuildItem);
            // if the handler is a proxy, then that means it's been produced by a recorder and therefore belongs in the regular runtime Vert.x instance
            // otherwise this is handled in the setupDeploymentSideHandling method
            if (!i.isDeploymentSide()) {
                if (devUIConfig.cors.enabled) {
                    routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                            .route("dev/*")
                            .handler(new DevConsoleCORSFilter())
                            .build());
                }
                NonApplicationRootPathBuildItem.Builder builder = nonApplicationRootPathBuildItem.routeBuilder()
                        .routeFunction(
                                "dev/" + groupAndArtifact.getKey() + "." + groupAndArtifact.getValue() + "/" + i.getPath(),
                                new RuntimeDevConsoleRoute(i.getMethod(),
                                        i.isBodyHandlerRequired() ? bodyHandlerBuildItem.getHandler() : null));
                if (i.isBlockingHandler()) {
                    builder.blockingRoute();
                }
                builder.handler(i.getHandler());
                routeBuildItemBuildProducer.produce(builder.build());
            }
        }

        DevConsoleManager.registerHandler(new DevConsoleHttpHandler());
        //must be last so the above routes have precedence
        routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route("dev/*")
                .handler(new DevConsoleFilter())
                .build());
        routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route("dev")
                .displayOnNotFoundPage("Dev UI")
                .handler(new RedirectHandler())
                .build());
    }

    @BuildStep
    void builder(Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem, BuildProducer<DevConsoleRouteBuildItem> producer) {
        if (effectiveIdeBuildItem.isPresent()) {
            producer.produce(new DevConsoleRouteBuildItem("openInIDE", "POST",
                    new OpenIdeHandler(effectiveIdeBuildItem.get().getIde())));
        }
    }

    static volatile ConsoleStateManager.ConsoleContext context;

    @Produce(ServiceStartBuildItem.class)
    @BuildStep()
    void setupConsole(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }
        if (context == null) {
            context = ConsoleStateManager.INSTANCE.createContext("HTTP");
        }
        Config c = ConfigProvider.getConfig();
        String host = c.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String port = c.getOptionalValue("quarkus.http.port", String.class).orElse("8080");
        context.reset(
                new ConsoleCommand('w', "Open the application in a browser", null, () -> openBrowser(rp, np, "/", host, port)),
                new ConsoleCommand('d', "Open the Dev UI in a browser", null, () -> openBrowser(rp, np, "/q/dev", host, port)));
    }

    public static void openBrowser(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, String path, String host,
            String port) {
        if (path.startsWith("/q")) {
            path = np.resolvePath(path.substring(3));
        } else {
            path = rp.resolvePath(path.substring(1));
        }

        StringBuilder sb = new StringBuilder("http://");
        Config c = ConfigProvider.getConfig();
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append(path);
        String url = sb.toString();

        Runtime rt = Runtime.getRuntime();
        OS os = OS.determineOS();
        String[] command = null;
        try {
            switch (os) {
                case MAC:
                    command = new String[] { "open", url };
                    break;
                case LINUX:
                    command = new String[] { "xdg-open", url };
                    break;
                case WINDOWS:
                    command = new String[] { "rundll32", "url.dll,FileProtocolHandler", url };
                    break;
                case OTHER:
                    log.error("Cannot launch browser on this operating system");
            }
            if (command != null) {
                rt.exec(command);
            }
        } catch (Exception e) {
            log.debug("Failed to launch browser", e);
            if (command != null) {
                log.warn("Unable to open browser using command: '" + String.join(" ", command) + "'. Failure is: '"
                        + e.getMessage() + "'");
            }
        }

    }

    private Engine buildEngine(List<DevTemplatePathBuildItem> devTemplatePaths,
            List<RouteBuildItem> allRoutes,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            LaunchModeBuildItem launchModeBuildItem) {
        EngineBuilder builder = Engine.builder().addDefaults();

        // Escape some characters for HTML templates
        builder.addResultMapper(new HtmlEscaper(List.of(Variant.TEXT_HTML)));

        builder.strictRendering(true)
                .addValueResolver(new ReflectionValueResolver())
                .addValueResolver(new JsonObjectValueResolver())
                .addValueResolver(new MultiMapValueResolver())
                .addValueResolver(ValueResolvers.rawResolver())
                .addNamespaceResolver(NamespaceResolver.builder("ideInfo")
                        .resolve(new IdeInfoContextFunction(buildSystemTargetBuildItem, effectiveIdeBuildItem,
                                launchModeBuildItem))
                        .build())
                .addNamespaceResolver(NamespaceResolver.builder("info").resolve(ctx -> {
                    String ext = DevConsole.currentExtension.get();
                    if (ext == null) {
                        return Results.NotFound.from(ctx);
                    }
                    Map<String, Object> map = DevConsoleManager.getTemplateInfo().get(ext);
                    if (map == null) {
                        return Results.NotFound.from(ctx);
                    }
                    Object result = map.get(ctx.getName());
                    return result == null ? Results.NotFound.from(ctx) : result;
                }).build());

        // Create map of resolved paths
        Map<String, String> resolvedPaths = new HashMap<>();
        for (RouteBuildItem item : allRoutes) {
            ConfiguredPathInfo resolvedPathBuildItem = item.getConfiguredPathInfo();
            if (resolvedPathBuildItem != null) {
                resolvedPaths.put(resolvedPathBuildItem.getName(),
                        resolvedPathBuildItem.getEndpointPath(nonApplicationRootPathBuildItem));
            }
        }

        Map<String, String> defaultValues = getDefaultValues(configDescriptionBuildItems);

        // {config:property('quarkus.lambda.handler')}
        // {config:http-path('quarkus.smallrye-graphql.ui.root-path')}
        // Note that the output value is always string!
        builder.addNamespaceResolver(NamespaceResolver.builder("config").resolveAsync(ctx -> {
            List<Expression> params = ctx.getParams();
            if (params.size() != 1 || (!ctx.getName().equals("property") && !ctx.getName().equals("http-path"))) {
                return Results.notFound(ctx);
            }
            if (ctx.getName().equals("http-path")) {
                return ctx.evaluate(params.get(0)).thenCompose(propertyName -> {
                    String value = resolvedPaths.get(propertyName.toString());
                    return CompletableFuture.completedFuture(value != null ? value : Results.NotFound.from(ctx));
                });
            } else {
                return ctx.evaluate(params.get(0)).thenCompose(propertyName -> {
                    // Use the function stored in the DevConsoleRecorder to make sure the config is obtained using the correct TCCL
                    Function<String, Optional<String>> configFun = DevConsoleManager.getGlobal("devui-config-fun");
                    String val = configFun.apply(propertyName.toString()).orElse(defaultValues.get(propertyName.toString()));
                    return CompletableFuture.completedFuture(val != null ? val : Results.NotFound.from(ctx));
                });
            }
        }).build());

        // JavaDoc formatting
        builder.addValueResolver(new JavaDocResolver());

        // Config/System property -> Environment variable
        builder.addValueResolver(new PropertyToEnvVarConverter());

        // Add templates and tags
        Map<String, String> templates = new HashMap<>();
        for (DevTemplatePathBuildItem devTemplatePath : devTemplatePaths) {
            templates.put(devTemplatePath.getPath(), devTemplatePath.getContents());
            if (devTemplatePath.isTag()) {
                // Strip suffix, item.html -> item
                String tagName = devTemplatePath.getTagName();
                builder.addSectionHelper(new UserTagSectionHelper.Factory(tagName, devTemplatePath.getPath()));
            }
        }
        builder.addLocator(id -> locateTemplate(id, templates));

        builder.addResultMapper(new ResultMapper() {
            @Override
            public int getPriority() {
                // The priority must be higher than the one used for HtmlEscaper
                return 10;
            }

            @Override
            public boolean appliesTo(Origin origin, Object result) {
                return Results.isNotFound(result);
            }

            @Override
            public String map(Object result, Expression expression) {
                Origin origin = expression.getOrigin();
                throw new TemplateException(origin,
                        String.format("Property not found in expression {%s} in template %s on line %s",
                                expression.toOriginalString(),
                                origin.getTemplateId(), origin.getLine()));
            }
        });
        builder.addResultMapper(new ResultMapper() {
            @Override
            public int getPriority() {
                // The priority must be higher than the one used for HtmlEscaper
                return 10;
            }

            @Override
            public boolean appliesTo(Origin origin, Object result) {
                return result.equals(EMPTY);
            }

            @Override
            public String map(Object result, Expression expression) {
                return "<<unset>>";
            }
        });

        Engine engine = builder.build();

        // preload all templates
        for (DevTemplatePathBuildItem devTemplatePath : devTemplatePaths) {
            if (!devTemplatePath.isTag()) {
                engine.getTemplate(devTemplatePath.getPath());
            }
        }
        return engine;
    }

    private Map<String, String> getDefaultValues(List<ConfigDescriptionBuildItem> configDescriptionBuildItems) {
        Map<String, String> defaultValues = new HashMap<>();

        for (ConfigDescriptionBuildItem configDescriptionBuildItem : configDescriptionBuildItems) {
            if (configDescriptionBuildItem.getDefaultValue() != null) {
                defaultValues.put(configDescriptionBuildItem.getPropertyName(), configDescriptionBuildItem.getDefaultValue());
            }
        }
        return defaultValues;
    }

    private static Optional<TemplateLocator.TemplateLocation> locateTemplate(String id, Map<String, String> templates) {
        String template = templates.get(id);
        if (template == null) {
            // Try path with suffixes
            for (String suffix : suffixes) {
                id = id + "." + suffix;
                template = templates.get(id);
                if (template != null) {
                    break;
                }
            }
        }

        if (template == null)
            return Optional.empty();

        String templateName = id;
        String finalTemplate = template;
        return Optional.of(new TemplateLocator.TemplateLocation() {
            @Override
            public Reader read() {
                return new StringReader(finalTemplate);
            }

            @Override
            public Optional<Variant> getVariant() {
                Variant variant = null;
                String fileName = templateName;
                int slashIdx = fileName.lastIndexOf('/');
                if (slashIdx != -1) {
                    fileName = fileName.substring(slashIdx, fileName.length());
                }
                int dotIdx = fileName.lastIndexOf('.');
                if (dotIdx != -1) {
                    String suffix = fileName.substring(dotIdx + 1, fileName.length());
                    if (suffix.equalsIgnoreCase("json")) {
                        variant = Variant.forContentType(Variant.APPLICATION_JSON);
                    } else {
                        String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
                        if (contentType != null) {
                            variant = Variant.forContentType(contentType);
                        }
                    }
                }
                return Optional.ofNullable(variant);
            }
        });
    }

    @BuildStep
    void collectTemplates(BuildProducer<DevTemplatePathBuildItem> devTemplatePaths) {
        for (ClassPathElement pe : QuarkusClassLoader.getElements("dev-templates", false)) {
            scanTemplates(pe, devTemplatePaths);
        }
    }

    private void scanTemplates(ClassPathElement cpe, BuildProducer<DevTemplatePathBuildItem> devTemplatePaths) {
        final ArtifactKey key = cpe.getDependencyKey();
        if (key == null) {
            return;
        }
        String prefix;
        // don't move stuff for our "root" dev console artifact, since it includes the main template
        if (key.getArtifactId().equals("quarkus-vertx-http-deployment") && key.getGroupId().equals("io.quarkus")) {
            prefix = "";
        } else {
            final StringBuilder sb = new StringBuilder()
                    .append(key.getGroupId()).append('.');
            if (key.getArtifactId().endsWith(ArtifactInfoUtil.DEPLOYMENT)) {
                sb.append(
                        key.getArtifactId().substring(0, key.getArtifactId().length() - ArtifactInfoUtil.DEPLOYMENT.length()));
            } else {
                sb.append(key.getArtifactId());
            }
            prefix = sb.append('/').toString();
        }
        cpe.apply(tree -> {
            final Path devTemplatesPath = tree.getPath("dev-templates");
            try {
                Files.walkFileTree(devTemplatesPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String contents = Files.readString(file);
                        // don't move tags yet, since we don't know how to use them afterwards
                        String relativePath = devTemplatesPath.relativize(file).toString();
                        String correctedPath;
                        if (File.separatorChar == '\\') {
                            relativePath = relativePath.replace('\\', '/');
                        }
                        if (relativePath.startsWith(DevTemplatePathBuildItem.TAGS)) {
                            correctedPath = relativePath;
                        } else {
                            correctedPath = prefix + relativePath;
                        }
                        devTemplatePaths.produce(new DevTemplatePathBuildItem(correctedPath, contents));
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        });
    }

    /**
     * Convert a config/system property name to an environment variable name
     */
    private static final class PropertyToEnvVarConverter implements ValueResolver {

        @Override
        public boolean appliesTo(EvalContext context) {
            return context.getBase() instanceof String && context.getName().equals("toEnvVar");
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return CompletableFuture.completedFuture(
                    StringUtil
                            .replaceNonAlphanumericByUnderscores(context.getBase().toString())
                            .toUpperCase());
        }
    }

    public static class JavaDocResolver implements ValueResolver {

        private final Pattern codePattern = Pattern.compile("(\\{@code )([^}]+)(\\})");
        private final Pattern linkPattern = Pattern.compile("(\\{@link )([^}]+)(\\})");

        @Override
        public boolean appliesTo(EvalContext context) {
            return context.getBase() instanceof String && context.getName().equals("fmtJavadoc");
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            String val = context.getBase().toString();
            // Replace {@code} and {@link}
            val = codePattern.matcher(val).replaceAll("<code>$2</code>");
            val = linkPattern.matcher(val).replaceAll("<code>$2</code>");
            // Add br before @see and @deprecated
            val = val.replace("@see", "<br><strong>@see</strong>").replace("@deprecated",
                    "<br><strong>@deprecated</strong>");
            // No need to escape special characters
            return CompletableFuture.completedFuture(new RawString(val));
        }
    }

    public static final class HistoryHandlerBuildItem extends SimpleBuildItem {
        final RuntimeValue<Optional<WebSocketLogHandler>> value;

        public HistoryHandlerBuildItem(RuntimeValue<Optional<WebSocketLogHandler>> value) {
            this.value = value;
        }
    }

    private static class DetectPackageFileVisitor extends SimpleFileVisitor<Path> {
        private final List<String> paths;

        public DetectPackageFileVisitor(List<String> paths) {
            this.paths = paths;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            boolean hasRegularFiles = false;
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        hasRegularFiles = true;
                        break;
                    }
                }
            }
            if (hasRegularFiles) {
                paths.add(dir.toAbsolutePath().toString());
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private static class IdeInfoContextFunction implements Function<EvalContext, Object> {

        private static final String[] SUPPORTED_LANGS = { "java", "kotlin" };

        private final Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem;
        private final boolean disable;

        public IdeInfoContextFunction(BuildSystemTargetBuildItem buildSystemTargetBuildItem,
                Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem,
                LaunchModeBuildItem launchModeBuildItem) {
            this.effectiveIdeBuildItem = effectiveIdeBuildItem;
            disable = launchModeBuildItem.getDevModeType().orElse(DevModeType.LOCAL) != DevModeType.LOCAL;
        }

        @Override
        public Object apply(EvalContext ctx) {
            String ctxName = ctx.getName();

            List<Path> sourcesDir = DevConsoleManager.getHotReplacementContext().getSourcesDir();
            if (ctxName.equals("sourcePackages")) {
                if (disable) {
                    return Collections.emptyList(); // we need this here because the result needs to be iterable
                }
                Map<String, List<String>> sourcePackagesByLang = new HashMap<>();

                for (Path sourcePaths : sourcesDir) {
                    String lang = sourcePaths.getFileName().toString();
                    List<String> packages = sourcePackagesForRoot(sourcePaths);
                    if (!packages.isEmpty()) {
                        sourcePackagesByLang.put(lang, packages);
                    }
                }
                return sourcePackagesByLang;
            }
            if (ctxName.equals("locationPackages")) {
                if (disable) {
                    return Collections.emptyList(); // we need this here because the result needs to be iterable
                }
                Map<String, List<String>> sourcePackagesByDir = new HashMap<>();

                for (Path sourcePaths : sourcesDir) {
                    List<String> packages = sourcePackagesForRoot(sourcePaths);
                    if (!packages.isEmpty()) {
                        sourcePackagesByDir.put(sourcePaths.toAbsolutePath().toString().replace("\\", "/"), packages);
                    }
                }
                return sourcePackagesByDir;
            }
            if (disable) { // all the other values are Strings
                return EMPTY;
            }

            switch (ctxName) {
                case "ideLinkType":
                    if (!effectiveIdeBuildItem.isPresent()) {
                        return "none";
                    }
                    return effectiveIdeBuildItem.get().getIde().equals(Ide.VSCODE) ? "client" : "server";
                case "ideClientLinkFormat":
                    if (!effectiveIdeBuildItem.isPresent()) {
                        return "unused";
                    }
                    if (effectiveIdeBuildItem.get().getIde() == Ide.VSCODE) {
                        return "vscode://file/{0}:{1}";
                    } else {
                        return "unused";
                    }
                case "ideServerLinkEndpoint":
                    if (!effectiveIdeBuildItem.isPresent()) {
                        return "unused";
                    }
                    return "/io.quarkus.quarkus-vertx-http/openInIDE";
            }
            return Results.notFound(ctx);
        }

        /**
         * Return the most general packages used in the application
         * <p>
         * TODO: this likely covers almost all typical use cases, but probably needs some tweaks for extreme corner cases
         */
        private List<String> sourcePackagesForRoot(Path langPath) {
            if (!Files.exists(langPath)) {
                return Collections.emptyList();
            }
            File[] rootFiles = langPath.toFile().listFiles();
            List<Path> rootPackages = new ArrayList<>(1);
            if (rootFiles != null) {
                for (File rootFile : rootFiles) {
                    if (rootFile.isDirectory()) {
                        rootPackages.add(rootFile.toPath());
                    }
                }
            }
            if (rootPackages.isEmpty()) {
                return List.of("");
            }
            List<String> result = new ArrayList<>(rootPackages.size());
            for (Path rootPackage : rootPackages) {
                List<String> paths = new ArrayList<>();
                SimpleFileVisitor<Path> simpleFileVisitor = new DetectPackageFileVisitor(paths);
                try {
                    Files.walkFileTree(rootPackage, simpleFileVisitor);
                    if (paths.isEmpty()) {
                        continue;
                    }
                    String commonPath = commonPath(paths);
                    String rootPackageStr = commonPath.replace(langPath.toAbsolutePath().toString(), "")
                            .replace(File.separator, ".");
                    if (rootPackageStr.startsWith(".")) {
                        rootPackageStr = rootPackageStr.substring(1);
                    }
                    if (rootPackageStr.endsWith(".")) {
                        rootPackageStr = rootPackageStr.substring(0, rootPackageStr.length() - 1);
                    }
                    result.add(rootPackageStr);
                } catch (IOException e) {
                    log.debug("Unable to determine the sources directories", e);
                    // just ignore it as it's not critical for the DevUI functionality
                }
            }
            return result;
        }

        private String commonPath(List<String> paths) {
            String commonPath = "";
            List<String[]> dirs = new ArrayList<>(paths.size());
            for (int i = 0; i < paths.size(); i++) {
                dirs.add(i, paths.get(i).split(Pattern.quote(File.separator)));
            }
            for (int j = 0; j < dirs.get(0).length; j++) {
                String thisDir = dirs.get(0)[j]; // grab the next directory name in the first path
                boolean allMatched = true;
                for (int i = 1; i < dirs.size() && allMatched; i++) { // look at the other paths
                    if (dirs.get(i).length < j) { //there is no directory
                        allMatched = false;
                        break;
                    }
                    allMatched = dirs.get(i)[j].equals(thisDir); //check if it matched
                }
                if (allMatched) {
                    commonPath += thisDir + File.separator;
                } else {
                    break;
                }
            }
            return commonPath;
        }
    }
}
