package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildException;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.proxy.ProxyConfiguration;
import io.quarkus.deployment.proxy.ProxyFactory;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

/**
 * A test extension for testing Quarkus internals, not intended for end user consumption
 */
public class QuarkusUnitTest
        implements BeforeAllCallback, AfterAllCallback, TestInstanceFactory, BeforeEachCallback, AfterEachCallback {

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    boolean started = false;

    private RuntimeRunner runtimeRunner;
    private Path deploymentDir;
    private Consumer<Throwable> assertException;
    private Supplier<JavaArchive> archiveProducer;
    private List<Consumer<BuildChainBuilder>> buildChainCustomizers = new ArrayList<>();
    private Runnable afterUndeployListener;
    private String logFileName;
    private static final Timer timeoutTimer = new Timer("Test thread dump timer");
    private volatile TimerTask timeoutTask;
    private Properties customApplicationProperties;

    private final RestAssuredURLManager restAssuredURLManager;

    public QuarkusUnitTest setExpectedException(Class<? extends Throwable> expectedException) {
        return assertException(t -> {
            assertEquals(expectedException,
                    t.getClass(), "Build failed with wrong exception");
        });
    }

    public QuarkusUnitTest() {
        this(false);
    }

    public static QuarkusUnitTest withSecuredConnection() {
        return new QuarkusUnitTest(true);
    }

    private QuarkusUnitTest(boolean useSecureConnection) {
        this.restAssuredURLManager = new RestAssuredURLManager(useSecureConnection);
    }

    public QuarkusUnitTest assertException(Consumer<Throwable> assertException) {
        this.assertException = assertException;
        return this;
    }

    public Supplier<JavaArchive> getArchiveProducer() {
        return archiveProducer;
    }

    public QuarkusUnitTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        Objects.requireNonNull(archiveProducer);
        this.archiveProducer = archiveProducer;
        return this;
    }

    public QuarkusUnitTest addBuildChainCustomizer(Consumer<BuildChainBuilder> customizer) {
        this.buildChainCustomizers.add(customizer);
        return this;
    }

    public QuarkusUnitTest setLogFileName(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
            throws TestInstantiationException {
        try {
            Class testClass = extensionContext.getRequiredTestClass();

            ExtensionContext.Store store = extensionContext.getStore(ExtensionContext.Namespace.GLOBAL);
            Object actualTestInstance = store.get(testClass.getName());
            if (actualTestInstance != null) { //happens if a deployment exception is expected
                TestHTTPResourceManager.inject(actualTestInstance);
            }
            ProxyFactory<?> proxyFactory = (ProxyFactory<?>) store.get(proxyFactoryKey(testClass));
            return proxyFactory.newInstance(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (assertException != null) {
                        return null;
                    }
                    Method realMethod = actualTestInstance.getClass().getMethod(method.getName(), method.getParameterTypes());
                    return realMethod.invoke(actualTestInstance, args);
                }
            });
        } catch (Exception e) {
            throw new TestInstantiationException("Unable to create test proxy", e);
        }
    }

    private void exportArchive(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = getArchiveProducerOrDefault();
            archive.addClass(testClass);
            if (customApplicationProperties != null) {
                archive.add(new PropertiesAsset(customApplicationProperties), "application.properties");
            }
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

            String exportPath = System.getProperty("quarkus.deploymentExportPath");
            if (exportPath != null) {
                File exportDir = new File(exportPath);
                if (exportDir.exists()) {
                    if (!exportDir.isDirectory()) {
                        throw new IllegalStateException("Export path is not a directory: " + exportPath);
                    }
                    try (Stream<Path> stream = Files.walk(exportDir.toPath())) {
                        stream.sorted(Comparator.reverseOrder()).map(Path::toFile)
                                .forEach(File::delete);
                    }
                } else if (!exportDir.mkdirs()) {
                    throw new IllegalStateException("Export path could not be created: " + exportPath);
                }
                File exportFile = new File(exportDir, archive.getName());
                archive.as(ZipExporter.class).exportTo(exportFile);
            }
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
        timeoutTimer.schedule(timeoutTask, 1000 * 60 * 5);
        if (logFileName != null) {
            PropertyTestUtil.setLogFileProperty(logFileName);
        } else {
            PropertyTestUtil.setLogFileProperty();
        }
        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        if (store.get(TestResourceManager.class.getName()) == null) {
            TestResourceManager manager = new TestResourceManager(extensionContext.getRequiredTestClass());
            manager.start();
            store.put(TestResourceManager.class.getName(), new ExtensionContext.Store.CloseableResource() {

                @Override
                public void close() throws Throwable {
                    manager.stop();
                }
            });
        }

        Class<?> testClass = extensionContext.getRequiredTestClass();

        if (store.get(proxyFactoryKey(testClass)) == null) {
            ProxyFactory<?> factory = new ProxyFactory<>(new ProxyConfiguration<>()
                    .setAnchorClass(testClass)
                    .setProxyNameSuffix("$$QuarkusUnitTestProxy")
                    .setClassLoader(new DefineClassVisibleClassLoader(testClass.getClassLoader()))
                    .setSuperClass((Class<Object>) testClass));
            store.put(proxyFactoryKey(testClass), factory);
        }

        try {
            deploymentDir = Files.createTempDirectory("quarkus-unit-test");

            exportArchive(deploymentDir, testClass);

            List<Consumer<BuildChainBuilder>> customizers = new ArrayList<>(buildChainCustomizers);

            try {
                //this is a bit of a hack to avoid requiring a dep on the arc extension,
                //as this would mean we cannot use this to test the extension
                Class<? extends BuildItem> buildItem = Class
                        .forName("io.quarkus.arc.deployment.AdditionalBeanBuildItem").asSubclass(BuildItem.class);
                customizers.add(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                try {
                                    Method factoryMethod = buildItem.getMethod("unremovableOf", Class.class);
                                    context.produce((BuildItem) factoryMethod.invoke(null, testClass));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }).produces(buildItem)
                                .build();
                    }
                });
            } catch (ClassNotFoundException e) {
                //ignore
            }

            final Path testLocation = PathTestHelper.getTestClassesLocation(testClass);

            runtimeRunner = RuntimeRunner.builder()
                    .setLaunchMode(LaunchMode.TEST)
                    .setClassLoader(testClass.getClassLoader())
                    .setTarget(deploymentDir)
                    .excludeFromIndexing(testLocation)
                    .setFrameworkClassesPath(testLocation)
                    .addChainCustomizers(customizers)
                    .build();

            try {
                runtimeRunner.run();
                if (assertException != null) {
                    fail("The build was expected to fail");
                }
                started = true;
                System.setProperty("test.url", TestHTTPResourceManager.getUri());
                Instance<?> factory;
                try {
                    factory = CDI.current()
                            .select(Class.forName(testClass.getName(), true, Thread.currentThread().getContextClassLoader()));
                } catch (Exception e) {
                    throw new TestInstantiationException("Failed to create test instance", e);
                }

                Object actualTest = factory.get();
                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(testClass.getName(), actualTest);
            } catch (Throwable e) {
                started = false;
                if (assertException != null) {
                    if (e instanceof RuntimeException) {
                        Throwable cause = e.getCause();
                        if (cause != null && cause instanceof BuildException) {
                            assertException.accept(cause.getCause());
                        } else if (cause != null) {
                            assertException.accept(cause);
                        } else {
                            fail("Unable to unwrap build exception from: " + e);
                        }
                    } else {
                        fail("Unable to unwrap build exception from: " + e);
                    }
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String proxyFactoryKey(Class<?> testClass) {
        return testClass + "proxyFactory";
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        try {
            if (runtimeRunner != null) {
                runtimeRunner.close();
            }
            if (afterUndeployListener != null) {
                afterUndeployListener.run();
            }
        } finally {
            timeoutTask.cancel();
            timeoutTask = null;
            if (deploymentDir != null) {
                Files.walkFileTree(deploymentDir, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        restAssuredURLManager.clearURL();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        restAssuredURLManager.setURL();
    }

    public Runnable getAfterUndeployListener() {
        return afterUndeployListener;
    }

    public QuarkusUnitTest setAfterUndeployListener(Runnable afterUndeployListener) {
        this.afterUndeployListener = afterUndeployListener;
        return this;
    }

    public QuarkusUnitTest withConfigurationResource(String resourceName) {
        if (customApplicationProperties == null) {
            customApplicationProperties = new Properties();
        }
        try {
            try (InputStream in = ClassLoader.getSystemResourceAsStream(resourceName)) {
                customApplicationProperties.load(in);
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Could not load resource: '" + resourceName + "'");
        }
    }

    public QuarkusUnitTest overrideConfigKey(final String propertyKey, final String propertyValue) {
        if (customApplicationProperties == null) {
            customApplicationProperties = new Properties();
        }
        customApplicationProperties.put(propertyKey, propertyValue);
        return this;
    }

    private static class PropertiesAsset implements Asset {
        private final Properties props;

        public PropertiesAsset(final Properties props) {
            this.props = props;
        }

        @Override
        public InputStream openStream() {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(128);
            try {
                props.store(outputStream, "Unit test Generated Application properties");
            } catch (IOException e) {
                throw new RuntimeException("Could not write application properties resource", e);
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}
