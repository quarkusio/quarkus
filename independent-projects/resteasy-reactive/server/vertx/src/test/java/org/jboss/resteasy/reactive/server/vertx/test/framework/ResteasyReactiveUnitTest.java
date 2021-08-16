package org.jboss.resteasy.reactive.server.vertx.test.framework;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.io.Closeable;
import java.io.IOException;
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
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveInterceptorScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.startup.CustomServerRestHandlers;
import org.jboss.resteasy.reactive.server.core.startup.RuntimeDeploymentManager;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveContextResolverScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveExceptionMappingScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveFeatureScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveParamConverterScanner;
import org.jboss.resteasy.reactive.server.providers.serialisers.ServerByteArrayMessageBodyHandler;
import org.jboss.resteasy.reactive.server.providers.serialisers.ServerStringMessageBodyHandler;
import org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveVertxHandler;
import org.jboss.resteasy.reactive.server.vertx.VertxRequestContextFactory;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ResteasyReactiveUnitTest implements BeforeAllCallback, AfterAllCallback {

    private static final Logger rootLogger;
    private Handler[] originalHandlers;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        rootLogger = LogManager.getLogManager().getLogger("");
    }

    private Path deploymentDir;
    private Consumer<Throwable> assertException;
    private Supplier<JavaArchive> archiveProducer;

    private Consumer<List<LogRecord>> assertLogRecords;

    private Timer timeoutTimer;
    private volatile TimerTask timeoutTask;
    private InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler((r) -> false);

    static Vertx vertx;
    static HttpServer httpServer;
    static Router router;
    static Route route;
    static Executor executor = Executors.newFixedThreadPool(10);

    List<Closeable> closeTasks = new ArrayList<>();

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

        if (vertx == null) {
            vertx = Vertx.vertx();
            HttpServer server = vertx.createHttpServer();
            router = Router.router(vertx);
            server.requestHandler(router).listen(8080).toCompletionStage().toCompletableFuture().get();
            store.put(ResteasyReactiveUnitTest.class.getName(), new ExtensionContext.Store.CloseableResource() {
                @Override
                public void close() throws Throwable {
                    server.close();
                    vertx.close();
                }
            });
        }

        Index index = JandexUtil.createIndex(deploymentDir);
        ApplicationScanningResult applicationScanningResult = ResteasyReactiveScanner.scanForApplicationClass(index,
                Collections.emptySet());
        ResourceScanningResult resources = ResteasyReactiveScanner.scanResources(index);
        if (resources == null) {
            throw new RuntimeException("no JAX-RS resources found");
        }
        ServerEndpointIndexer serverEndpointIndexer = new ServerEndpointIndexer.Builder()
                .setIndex(index)
                .setScannedResourcePaths(resources.getScannedResourcePaths())
                .setClassLevelExceptionMappers(new HashMap<>())
                .setInjectableBeans(new HashMap<>())
                .setConfig(new ResteasyReactiveConfig(10000, true, true))
                .setHttpAnnotationToMethod(resources.getHttpAnnotationToMethod())
                .build();

        List<ResourceClass> resourceClasses = new ArrayList<>();
        List<ResourceClass> subResourceClasses = new ArrayList<>();
        for (Map.Entry<DotName, ClassInfo> i : resources.getScannedResources().entrySet()) {
            ResourceClass res = serverEndpointIndexer.createEndpoints(i.getValue());
            resourceClasses.add(res);
        }
        for (Map.Entry<DotName, ClassInfo> i : resources.getPossibleSubResources().entrySet()) {
            ResourceClass res = serverEndpointIndexer.createEndpoints(i.getValue());
            subResourceClasses.add(res);
        }

        ServerSerialisers serialisers = new ServerSerialisers();
        serialisers.addWriter(String.class, new ResourceWriter()
                .setMediaTypeStrings(Collections.singletonList(MediaType.WILDCARD))
                .setFactory(new BeanFactory<MessageBodyWriter<?>>() {
                    @Override
                    public BeanInstance<MessageBodyWriter<?>> createInstance() {
                        return new BeanInstance<MessageBodyWriter<?>>() {
                            @Override
                            public MessageBodyWriter<?> getInstance() {
                                return new ServerStringMessageBodyHandler();
                            }

                            @Override
                            public void close() {

                            }
                        };
                    }
                }));
        serialisers.addReader(byte[].class, new ResourceReader()
                .setMediaTypeStrings(Collections.singletonList(MediaType.WILDCARD))
                .setFactory(new BeanFactory<MessageBodyReader<?>>() {
                    @Override
                    public BeanInstance<MessageBodyReader<?>> createInstance() {
                        return new BeanInstance<MessageBodyReader<?>>() {
                            @Override
                            public MessageBodyReader<?> getInstance() {
                                return new ServerByteArrayMessageBodyHandler();
                            }

                            @Override
                            public void close() {

                            }
                        };
                    }
                }));
        DeploymentInfo info = new DeploymentInfo()
                .setApplicationPath("/")
                .setConfig(new ResteasyReactiveConfig())
                .setFeatures(ResteasyReactiveFeatureScanner.createFeatures(index, applicationScanningResult))
                .setInterceptors(
                        ResteasyReactiveInterceptorScanner.createResourceInterceptors(index, applicationScanningResult))
                .setDynamicFeatures(ResteasyReactiveFeatureScanner.createDynamicFeatures(index, applicationScanningResult))
                .setParamConverterProviders(
                        ResteasyReactiveParamConverterScanner.createParamConverters(index, applicationScanningResult))
                .setSerialisers(serialisers)
                .setExceptionMapping(
                        ResteasyReactiveExceptionMappingScanner.createExceptionMappers(index, applicationScanningResult))
                .setResourceClasses(resourceClasses)
                .setCtxResolvers(
                        ResteasyReactiveContextResolverScanner.createContextResolvers(index, applicationScanningResult))
                .setLocatableResourceClasses(subResourceClasses)
                .setApplicationSupplier(new Supplier<Application>() {
                    @Override
                    public Application get() {
                        if (applicationScanningResult.getSelectedAppClass() == null) {
                            return new Application();
                        } else {
                            try {
                                return (Application) Class
                                        .forName(applicationScanningResult.getSelectedAppClass().name().toString(), false,
                                                Thread.currentThread().getContextClassLoader())
                                        .newInstance();
                            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
        RuntimeDeploymentManager runtimeDeploymentManager = new RuntimeDeploymentManager(info, () -> executor,
                new CustomServerRestHandlers(null),
                closeable -> closeTasks.add(closeable), new VertxRequestContextFactory(), ThreadSetupAction.NOOP, "/");
        Deployment deployment = runtimeDeploymentManager.deploy();
        RestInitialHandler initialHandler = new RestInitialHandler(deployment);
        router.route().handler(new ResteasyReactiveVertxHandler(initialHandler));

    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
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
        router.clear();

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
