package org.jboss.resteasy.reactive.server.vertx.test.framework;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.ws.rs.core.MediaType;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.scanning.ScannedSerializer;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectiveContextInjectedBeanFactory;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.processor.ScannedApplication;
import org.jboss.resteasy.reactive.server.processor.generation.converters.GeneratedConvertersFeature;
import org.jboss.resteasy.reactive.server.processor.generation.exceptionmappers.ServerExceptionMappingFeature;
import org.jboss.resteasy.reactive.server.processor.generation.filters.FilterFeature;
import org.jboss.resteasy.reactive.server.processor.generation.injection.FieldInjectionFeature;
import org.jboss.resteasy.reactive.server.processor.generation.multipart.MultipartFeature;
import org.jboss.resteasy.reactive.server.processor.scanning.AsyncReturnTypeScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.FeatureScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResponseHeaderMethodScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResponseStatusMethodScanner;
import org.jboss.resteasy.reactive.server.spi.DefaultRuntimeConfiguration;
import org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveVertxHandler;
import org.jboss.resteasy.reactive.server.vertx.VertxRequestContextFactory;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerMutinyAsyncFileMessageBodyWriter;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerVertxAsyncFileMessageBodyWriter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public class ResteasyReactiveUnitTest implements BeforeAllCallback, AfterAllCallback, InvocationInterceptor {

    public static final int SERVER_PORT = 8080;

    public static final DotName HTTP_SERVER_REQUEST = DotName.createSimple(HttpServerRequest.class.getName());
    public static final DotName HTTP_SERVER_RESPONSE = DotName.createSimple(HttpServerResponse.class.getName());
    public static final DotName ROUTING_CONTEXT = DotName.createSimple(RoutingContext.class.getName());
    private static final Logger rootLogger;
    public static final String EXECUTOR_THREAD_NAME = "blocking executor thread";

    private Handler[] originalHandlers;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        rootLogger = LogManager.getLogManager().getLogger("");
        BlockingOperationSupport.setIoThreadDetector(new BlockingOperationSupport.IOThreadDetector() {
            @Override
            public boolean isBlockingAllowed() {
                return !Context.isOnEventLoopThread();
            }
        });
    }

    private Path deploymentDir;
    private Consumer<Throwable> assertException;
    private Supplier<JavaArchive> archiveProducer;

    private Consumer<List<LogRecord>> assertLogRecords;

    private Timer timeoutTimer;
    private volatile TimerTask timeoutTask;
    private InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler((r) -> false);

    static ClassLoader originalClassLoader;
    static String verticleId;
    static Vertx vertx;
    static ExecutorService executor;
    boolean deleteUploadedFilesOnEnd = true;
    Path uploadPath;

    private List<Consumer<ResteasyReactiveDeploymentManager.ScanStep>> scanCustomizers = new ArrayList<>();
    private List<Consumer<Route>> routeCustomisers = new ArrayList<>();

    List<Closeable> closeTasks = new ArrayList<>();
    private Charset defaultCharset = StandardCharsets.UTF_8;;
    private int maxFormAttributeSize = 2048;

    public static Vertx getVertx() {
        return vertx;
    }

    public ResteasyReactiveUnitTest addScanCustomizer(Consumer<ResteasyReactiveDeploymentManager.ScanStep> consumer) {
        scanCustomizers.add(consumer);
        return this;
    }

    public ResteasyReactiveUnitTest addRouteCustomizer(Consumer<Route> consumer) {
        routeCustomisers.add(consumer);
        return this;
    }

    public ResteasyReactiveUnitTest setDeleteUploadedFilesOnEnd(boolean deleteUploadedFilesOnEnd) {
        this.deleteUploadedFilesOnEnd = deleteUploadedFilesOnEnd;
        return this;
    }

    public ResteasyReactiveUnitTest setUploadPath(Path uploadPath) {
        this.uploadPath = uploadPath;
        return this;
    }

    public ResteasyReactiveUnitTest setDefaultCharset(Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
        return this;
    }

    public ResteasyReactiveUnitTest setMaxFormAttributeSize(int maxFormAttributeSize) {
        this.maxFormAttributeSize = maxFormAttributeSize;
        return this;
    }

    private static Executor setVirtualThreadExecutor() {
        Executor exec = Executors.newSingleThreadExecutor();
        try {
            exec = (Executor) Class.forName("java.util.concurrent.Executors")
                    .getMethod("newVirtualThreadPerTaskExecutor")
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        return exec;
    }

    public ResteasyReactiveUnitTest setExpectedException(Class<? extends Throwable> expectedException) {
        return assertException(t -> {
            Throwable i = t;
            boolean found = false;
            while (i != null) {
                if (i.getClass().getName().equals(expectedException.getName())) {
                    found = true;
                    break;
                }
                i = i.getCause();
            }

            assertTrue(found, "Build failed with wrong exception, expected " + expectedException + " but got " + t);
        });
    }

    public ResteasyReactiveUnitTest() {
    }

    public ResteasyReactiveUnitTest assertException(Consumer<Throwable> assertException) {
        if (this.assertException != null) {
            throw new IllegalStateException("Don't set the asserted or excepted exception twice"
                    + " to avoid shadowing out the first call.");
        }
        this.assertException = assertException;
        return this;
    }

    public Supplier<JavaArchive> getArchiveProducer() {
        return archiveProducer;
    }

    public ResteasyReactiveUnitTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        Objects.requireNonNull(archiveProducer);
        this.archiveProducer = archiveProducer;
        return this;
    }

    public ResteasyReactiveUnitTest assertLogRecords(Consumer<List<LogRecord>> assertLogRecords) {
        if (this.assertLogRecords != null) {
            throw new IllegalStateException("Don't set the a log record assertion twice"
                    + " to avoid shadowing out the first call.");
        }
        this.assertLogRecords = assertLogRecords;
        return this;
    }

    /**
     * Customize the application root.
     *
     * @param applicationRootConsumer
     * @return self
     */
    public ResteasyReactiveUnitTest withApplicationRoot(Consumer<JavaArchive> applicationRootConsumer) {
        Objects.requireNonNull(applicationRootConsumer);
        return setArchiveProducer(() -> {
            JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
            applicationRootConsumer.accept(jar);
            return jar;
        });
    }

    private void exportArchive(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = getArchiveProducerOrDefault();
            Class<?> c = testClass;
            archive.addClasses(c.getClasses());
            while (c != Object.class) {
                archive.addClass(c);
                c = c.getSuperclass();
            }
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

        } catch (Exception e) {
            throw new RuntimeException("Unable to create the archive", e);
        }
    }

    private JavaArchive getArchiveProducerOrDefault() {
        if (archiveProducer == null) {
            return ShrinkWrap.create(JavaArchive.class);
        } else {
            return archiveProducer.get();
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        originalHandlers = rootLogger.getHandlers();

        timeoutTask = new TimerTask() {
            @Override
            public void run() {
                System.err.println("Test has been running for more than 5 minutes, thread dump is:");
                for (Map.Entry<Thread, StackTraceElement[]> i : Thread.getAllStackTraces().entrySet()) {
                    System.err.println("\n");
                    System.err.println(i.toString());
                    System.err.println("\n");
                    for (StackTraceElement j : i.getValue()) {
                        System.err.println(j);
                    }
                }
            }
        };
        timeoutTimer = new Timer("Test thread dump timer");
        timeoutTimer.schedule(timeoutTask, 1000 * 60 * 5);
        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);

        Class<?> testClass = extensionContext.getRequiredTestClass();

        deploymentDir = Files.createTempDirectory("quarkus-unit-test");

        exportArchive(deploymentDir, testClass);

        try {
            doDeployment();
        } catch (Throwable t) {
            if (assertException == null) {
                throw t;
            }
            assertException.accept(t);
        }

    }

    private void doDeployment() throws MalformedURLException, InterruptedException, ExecutionException, ClassNotFoundException {
        FieldInjectionFeature fieldInjectionSupport = new FieldInjectionFeature();
        IndexView nonCalcIndex = JandexUtil.createIndex(deploymentDir);
        ResteasyReactiveDeploymentManager.ScanStep scanStep = ResteasyReactiveDeploymentManager.start(nonCalcIndex);
        scanStep.addContextTypes(Set.of(HTTP_SERVER_REQUEST, HTTP_SERVER_RESPONSE, ROUTING_CONTEXT));
        scanStep.addFeatureScanner(new MultipartFeature());
        scanStep.addFeatureScanner(new GeneratedConvertersFeature());
        scanStep.addFeatureScanner(fieldInjectionSupport);
        scanStep.addMethodScanner(new AsyncReturnTypeScanner());
        scanStep.addMethodScanner(new ResponseStatusMethodScanner());
        scanStep.addMethodScanner(new ResponseHeaderMethodScanner());
        scanStep.addFeatureScanner(new FeatureScanner() {
            @Override
            public FeatureScanResult integrate(IndexView application, ScannedApplication scannedApplication) {
                scannedApplication.getSerializerScanningResult().getWriters()
                        .add(new ScannedSerializer(ServerMutinyAsyncFileMessageBodyWriter.class.getName(),
                                AsyncFile.class.getName(), List.of(MediaType.WILDCARD)));
                scannedApplication.getSerializerScanningResult().getWriters()
                        .add(new ScannedSerializer(ServerVertxAsyncFileMessageBodyWriter.class.getName(),
                                io.vertx.core.file.AsyncFile.class.getName(), List.of(MediaType.WILDCARD)));
                return new FeatureScanResult(List.of());
            }
        });
        scanStep.addFeatureScanner(
                new FilterFeature(Set.of(HTTP_SERVER_REQUEST, HTTP_SERVER_RESPONSE, ROUTING_CONTEXT), Set.of()));
        scanStep.addFeatureScanner(
                new ServerExceptionMappingFeature(Set.of(HTTP_SERVER_REQUEST, HTTP_SERVER_RESPONSE, ROUTING_CONTEXT),
                        Set.of()));
        scanCustomizers.forEach((c) -> c.accept(scanStep));

        ResteasyReactiveDeploymentManager.ScanResult scanned = scanStep.scan();

        ResteasyReactiveTestClassLoader testClassLoader = new ResteasyReactiveTestClassLoader(
                new URL[] { deploymentDir.toUri().toURL() }, Thread.currentThread().getContextClassLoader(),
                scanned.getGeneratedClasses(), scanned.getTransformers());
        Thread.currentThread().setContextClassLoader(testClassLoader);
        vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        verticleId = vertx.deployVerticle(new Verticle() {
            private HttpServer server;

            @Override
            public Vertx getVertx() {
                return vertx;
            }

            @Override
            public void init(Vertx vertx, Context context) {

            }

            @Override
            public void start(Promise<Void> startPromise) throws Exception {
                server = vertx.createHttpServer();
                server.requestHandler(router).listen(SERVER_PORT)
                        .onComplete(new io.vertx.core.Handler<AsyncResult<HttpServer>>() {
                            @Override
                            public void handle(AsyncResult<HttpServer> event) {
                                if (event.failed()) {
                                    startPromise.fail(event.cause());
                                } else {
                                    startPromise.complete();
                                }
                            }
                        });
            }

            @Override
            public void stop(Promise<Void> stopPromise) throws Exception {
                server.close().onComplete(stopPromise);
            }
        }, new DeploymentOptions().setClassLoader(testClassLoader)).toCompletionStage().toCompletableFuture().get();

        executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, EXECUTOR_THREAD_NAME);
            }
        });

        ResteasyReactiveDeploymentManager.PreparedApplication prepared = scanned
                .prepare(testClassLoader, ReflectiveContextInjectedBeanFactory.STRING_FACTORY);

        prepared.addScannedSerializers();
        prepared.addBuiltinSerializers();
        DefaultRuntimeConfiguration runtimeConfiguration = new DefaultRuntimeConfiguration(Duration.ofMinutes(1),
                deleteUploadedFilesOnEnd,
                uploadPath != null ? uploadPath.toAbsolutePath().toString() : System.getProperty("java.io.tmpdir"),
                defaultCharset, Optional.empty(), maxFormAttributeSize);
        ResteasyReactiveDeploymentManager.RunnableApplication application = prepared.createApplication(runtimeConfiguration,
                new VertxRequestContextFactory(), executor);
        fieldInjectionSupport.runtimeInit(testClassLoader, application.getDeployment());

        ResteasyReactiveVertxHandler handler = new ResteasyReactiveVertxHandler(application.getInitialHandler());
        String path = application.getPath();
        Route route = router.route(path);
        for (Consumer<Route> customizer : routeCustomisers) {
            customizer.accept(route);
        }
        route.handler(handler);
        Route starRoute;
        if (path.endsWith("/")) {
            starRoute = router.route(path + "*");
        } else {
            starRoute = router.route(path + "/*");
        }
        for (Consumer<Route> customizer : routeCustomisers) {
            customizer.accept(starRoute);
        }
        starRoute.handler(handler);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        try {
            if (assertLogRecords != null) {
                assertLogRecords.accept(inMemoryLogHandler.records);
            }
            //rootLogger.setHandlers(originalHandlers);
            inMemoryLogHandler.clearRecords();

            System.clearProperty("test.url");
            timeoutTask.cancel();
            timeoutTask = null;
            timeoutTimer = null;
            if (deploymentDir != null) {
                deleteDirectory(deploymentDir);
            }
        } finally {
            if (executor != null) {
                executor.shutdown();
                executor = null;
            }
            if (verticleId != null) {
                vertx.undeploy(verticleId).toCompletionStage().toCompletableFuture().get();
                verticleId = null;
            }
            if (vertx != null) {
                vertx.close().toCompletionStage().toCompletableFuture().get();
                vertx = null;
            }
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            invocation.proceed();
        } else {
            invocation.skip();
        }
    }

    public static void deleteDirectory(final Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

        });
    }

}
