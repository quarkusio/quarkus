package io.quarkus.vertx.http.deployment.devmode.console;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.deployment.util.ArtifactInfoUtil;
import io.quarkus.deployment.util.WebJarUtil;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
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
import io.quarkus.qute.Results;
import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.ValueResolvers;
import io.quarkus.qute.Variant;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleFilter;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleRecorder;
import io.quarkus.vertx.http.runtime.devmode.RedirectHandler;
import io.quarkus.vertx.http.runtime.devmode.RuntimeDevConsoleRoute;
import io.quarkus.vertx.http.runtime.logstream.HistoryHandler;
import io.quarkus.vertx.http.runtime.logstream.LogStreamRecorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.impl.VertxHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class DevConsoleProcessor {

    private static final Logger log = Logger.getLogger(DevConsoleProcessor.class);

    private static final String STATIC_RESOURCES_PATH = "dev-static/";

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
        devConsoleVertx = Vertx.vertx();
        VertxInternal vertx = (VertxInternal) devConsoleVertx;
        QuarkusClassLoader ccl = (QuarkusClassLoader) DevConsoleProcessor.class.getClassLoader();
        ccl.addCloseTask(new Runnable() {
            @Override
            public void run() {
                virtualBootstrap = null;
                if (devConsoleVertx != null) {
                    devConsoleVertx.close();
                    devConsoleVertx = null;
                }
                if (channel != null) {
                    try {
                        channel.close().sync();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("failed to close virtual http");
                    }
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
                        ContextInternal context = (ContextInternal) vertx
                                .createEventLoopContext(null, null, new JsonObject(),
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
                            conn.handler(new Handler<HttpServerRequest>() {
                                @Override
                                public void handle(HttpServerRequest event) {
                                    mainRouter.handle(event);
                                }
                            });
                            return conn;
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

    protected static void newRouter(Engine engine,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        // "/" or "/myroot/"
        String httpRootPath = nonApplicationRootPathBuildItem.getNormalizedHttpRootPath();
        // "/" or "/myroot/" or "/q/" or "/myroot/q/"
        String frameworkRootPath = nonApplicationRootPathBuildItem.getNonApplicationRootPath();

        Handler<RoutingContext> errorHandler = new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                log.error("Dev console request failed ", event.failure());
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
        mainRouter.route(nonApplicationRootPathBuildItem.resolvePath("dev/*")).subRouter(router);
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
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    public HistoryHandlerBuildItem handler(BuildProducer<LogHandlerBuildItem> logHandlerBuildItemBuildProducer,
            LogStreamRecorder recorder, DevUIConfig devUiConfig) {
        RuntimeValue<Optional<HistoryHandler>> handler = recorder.handler(devUiConfig.historySize);
        logHandlerBuildItemBuildProducer.produce(new LogHandlerBuildItem((RuntimeValue) handler));
        return new HistoryHandlerBuildItem(handler);
    }

    @Consume(LoggingSetupBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    public ServiceStartBuildItem setupDeploymentSideHandling(List<DevTemplatePathBuildItem> devTemplatePaths,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<RouteBuildItem> allRoutes,
            List<DevConsoleRouteBuildItem> routes,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem, LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return null;
        }

        initializeVirtual();
        Engine quteEngine = buildEngine(devTemplatePaths,
                allRoutes,
                nonApplicationRootPathBuildItem);
        newRouter(quteEngine, nonApplicationRootPathBuildItem);

        for (DevConsoleRouteBuildItem i : routes) {
            Entry<String, String> groupAndArtifact = i.groupIdAndArtifactId(curateOutcomeBuildItem);
            // deployment side handling
            if (!(i.getHandler() instanceof BytecodeRecorderImpl.ReturnedProxy)) {
                router.route(HttpMethod.valueOf(i.getMethod()),
                        "/" + groupAndArtifact.getKey() + "." + groupAndArtifact.getValue() + "/" + i.getPath())
                        .handler(i.getHandler());
            }
        }

        return null;
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(LoggingSetupBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    public void setupDevConsoleRoutes(List<DevConsoleRouteBuildItem> routes,
            BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            LogStreamRecorder recorder,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            HistoryHandlerBuildItem historyHandlerBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }

        // Add the log stream
        routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route("dev/logstream")
                .handler(recorder.websocketHandler(historyHandlerBuildItem.value))
                .build());

        for (DevConsoleRouteBuildItem i : routes) {
            Entry<String, String> groupAndArtifact = i.groupIdAndArtifactId(curateOutcomeBuildItem);
            // if the handler is a proxy, then that means it's been produced by a recorder and therefore belongs in the regular runtime Vert.x instance
            // otherwise this is handled in the setupDeploymentSideHandling method
            if (i.getHandler() instanceof BytecodeRecorderImpl.ReturnedProxy) {
                routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                        .routeFunction(
                                "dev/" + groupAndArtifact.getKey() + "." + groupAndArtifact.getValue() + "/" + i.getPath(),
                                new RuntimeDevConsoleRoute(i.getMethod()))
                        .handler(i.getHandler())
                        .build());
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

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void deployStaticResources(DevConsoleRecorder recorder, CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdownContext,
            BuildProducer<RouteBuildItem> routeBuildItemBuildProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchModeBuildItem) throws IOException {

        if (launchModeBuildItem.getDevModeType().orElse(DevModeType.LOCAL) != DevModeType.LOCAL) {
            return;
        }
        AppArtifact devConsoleResourcesArtifact = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, "io.quarkus",
                "quarkus-vertx-http-deployment");

        Path devConsoleStaticResourcesDeploymentPath = WebJarUtil.copyResourcesForDevOrTest(curateOutcomeBuildItem, launchMode,
                devConsoleResourcesArtifact, STATIC_RESOURCES_PATH);

        routeBuildItemBuildProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route("dev/resources/*")
                .handler(recorder.devConsoleHandler(devConsoleStaticResourcesDeploymentPath.toString(), shutdownContext))
                .build());
    }

    private Engine buildEngine(List<DevTemplatePathBuildItem> devTemplatePaths,
            List<RouteBuildItem> allRoutes,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        EngineBuilder builder = Engine.builder().addDefaults();

        // Escape some characters for HTML templates
        builder.addResultMapper(new HtmlEscaper());

        builder.addValueResolver(new ReflectionValueResolver())
                .addValueResolver(new JsonObjectValueResolver())
                .addValueResolver(ValueResolvers.rawResolver())
                .addNamespaceResolver(NamespaceResolver.builder("info").resolve(ctx -> {
                    String ext = DevConsole.currentExtension.get();
                    if (ext == null) {
                        return Results.Result.NOT_FOUND;
                    }
                    Map<String, Object> map = DevConsoleManager.getTemplateInfo().get(ext);
                    if (map == null) {
                        return Results.Result.NOT_FOUND;
                    }
                    Object result = map.get(ctx.getName());
                    return result == null ? Results.Result.NOT_FOUND : result;
                }).build());

        // Create map of resolved paths
        Map<String, String> resolvedPaths = new HashMap<>();
        for (RouteBuildItem item : allRoutes) {
            ConfiguredPathInfo resolvedPathBuildItem = item.getDevConsoleResolvedPath();
            if (resolvedPathBuildItem != null) {
                resolvedPaths.put(resolvedPathBuildItem.getName(),
                        resolvedPathBuildItem.getEndpointPath(nonApplicationRootPathBuildItem));
            }
        }

        // {config:property('quarkus.lambda.handler')}
        // {config:http-path('quarkus.smallrye-graphql.ui.root-path')}
        // Note that the output value is always string!
        builder.addNamespaceResolver(NamespaceResolver.builder("config").resolveAsync(ctx -> {
            List<Expression> params = ctx.getParams();
            if (params.size() != 1 || (!ctx.getName().equals("property") && !ctx.getName().equals("http-path"))) {
                return Results.NOT_FOUND;
            }
            if (ctx.getName().equals("http-path")) {
                return ctx.evaluate(params.get(0)).thenCompose(propertyName -> {
                    String value = resolvedPaths.get(propertyName.toString());
                    return CompletableFuture.completedFuture(value != null ? value : Result.NOT_FOUND);
                });
            } else {
                return ctx.evaluate(params.get(0)).thenCompose(propertyName -> {
                    Optional<String> val = ConfigProvider.getConfig().getOptionalValue(propertyName.toString(), String.class);
                    return CompletableFuture.completedFuture(val.isPresent() ? val.get() : Result.NOT_FOUND);
                });
            }
        }).build());

        // JavaDoc formatting
        builder.addValueResolver(new JavaDocResolver());

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

        Engine engine = builder.build();

        // pre-load all templates
        for (DevTemplatePathBuildItem devTemplatePath : devTemplatePaths) {
            if (!devTemplatePath.isTag()) {
                engine.getTemplate(devTemplatePath.getPath());
            }
        }
        return engine;
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
        try {
            ClassLoader classLoader = DevConsoleProcessor.class.getClassLoader();
            Enumeration<URL> devTemplateURLs = classLoader.getResources("/dev-templates");
            while (devTemplateURLs.hasMoreElements()) {
                String devTemplatesURL = devTemplateURLs.nextElement().toExternalForm();
                if (devTemplatesURL.startsWith("jar:file:") && devTemplatesURL.endsWith("!/dev-templates")) {
                    String jarPath = devTemplatesURL.substring(9, devTemplatesURL.length() - 15);
                    if (File.separatorChar == '\\') {
                        // on Windows this will be /C:/some/path, so turn it into C:\some\path
                        jarPath = jarPath.substring(1).replace('/', '\\');
                    }
                    try (FileSystem fs = FileSystems
                            .newFileSystem(Paths.get(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name())), classLoader)) {
                        scanTemplates(fs, devTemplatePaths);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void scanTemplates(FileSystem fs, BuildProducer<DevTemplatePathBuildItem> devTemplatePaths) throws IOException {
        Entry<String, String> entry = ArtifactInfoUtil.groupIdAndArtifactId(fs);
        if (entry == null) {
            throw new RuntimeException("Artifact at " + fs + " is missing pom metadata");
        }
        String prefix;
        // don't move stuff for our "root" dev console artifact, since it includes the main template
        if (entry.getKey().equals("io.quarkus")
                && entry.getValue().equals("quarkus-vertx-http"))
            prefix = "";
        else
            prefix = entry.getKey() + "." + entry.getValue() + "/";
        for (Path root : fs.getRootDirectories()) {
            Path devTemplatesPath = fs.getPath("/dev-templates");
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.toString().equals("/") || dir.startsWith(devTemplatesPath))
                        return FileVisitResult.CONTINUE;
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String contents = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    // don't move tags yet, since we don't know how to use them afterwards
                    String relativePath = devTemplatesPath.relativize(file).toString();
                    String correctedPath;
                    if (relativePath.startsWith(DevTemplatePathBuildItem.TAGS))
                        correctedPath = relativePath;
                    else
                        correctedPath = prefix + relativePath;
                    devTemplatePaths
                            .produce(new DevTemplatePathBuildItem(correctedPath, contents));
                    return super.visitFile(file, attrs);
                }
            });
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
        final RuntimeValue<Optional<HistoryHandler>> value;

        public HistoryHandlerBuildItem(RuntimeValue<Optional<HistoryHandler>> value) {
            this.value = value;
        }
    }
}
