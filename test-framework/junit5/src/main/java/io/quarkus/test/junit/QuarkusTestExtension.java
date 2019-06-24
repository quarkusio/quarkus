package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.platform.commons.JUnitException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.BootstrapClassLoaderFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runner.TransformerTarget;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestInjectionManager;
import io.quarkus.test.common.TestInstantiator;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

public class QuarkusTestExtension
        implements BeforeEachCallback, AfterEachCallback, TestInstanceFactory, BeforeAllCallback {

    private URLClassLoader appCl;
    private ClassLoader originalCl;
    private static boolean failedBoot;

    private final RestAssuredURLManager restAssuredURLManager = new RestAssuredURLManager(false);

    private ExtensionState doJavaStart(ExtensionContext context, TestResourceManager testResourceManager) {

        final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();

        Path appClassLocation = getAppClassLocation(context.getRequiredTestClass());

        try {
            appCl = BootstrapClassLoaderFactory.newInstance()
                    .setAppClasses(appClassLocation)
                    .setParent(getClass().getClassLoader())
                    .setOffline(PropertyUtils.getBooleanOrNull(BootstrapClassLoaderFactory.PROP_OFFLINE))
                    .setLocalProjectsDiscovery(
                            PropertyUtils.getBoolean(BootstrapClassLoaderFactory.PROP_WS_DISCOVERY, true))
                    .setEnableClasspathCache(PropertyUtils.getBoolean(BootstrapClassLoaderFactory.PROP_CP_CACHE, true))
                    .newDeploymentClassLoader();
        } catch (BootstrapException e) {
            throw new IllegalStateException("Failed to create the boostrap class loader", e);
        }
        originalCl = setCCL(appCl);

        final Path testClassLocation = getTestClassesLocation(context.getRequiredTestClass());
        final ClassLoader testClassLoader = context.getRequiredTestClass().getClassLoader();
        final Path testWiringClassesDir;
        final RuntimeRunner.Builder runnerBuilder = RuntimeRunner.builder();

        if (Files.isDirectory(testClassLocation)) {
            testWiringClassesDir = testClassLocation;
        } else {
            runnerBuilder.addAdditionalArchive(testClassLocation);
            testWiringClassesDir = Paths.get("").normalize().toAbsolutePath().resolve("target").resolve("test-classes");
            if (Files.exists(testWiringClassesDir)) {
                IoUtils.recursiveDelete(testWiringClassesDir);
            }
            try {
                Files.createDirectories(testWiringClassesDir);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to create a directory for wiring test classes at " + testWiringClassesDir, e);
            }
        }

        RuntimeRunner runtimeRunner = runnerBuilder
                .setLaunchMode(LaunchMode.TEST)
                .setClassLoader(appCl)
                .setTarget(appClassLocation)
                .addAdditionalArchive(testWiringClassesDir)
                .setClassOutput(new ClassOutput() {
                    @Override
                    public void writeClass(boolean applicationClass, String className, byte[] data) throws IOException {
                        Path location = testWiringClassesDir.resolve(className.replace('.', '/') + ".class");
                        Files.createDirectories(location.getParent());
                        try (FileOutputStream out = new FileOutputStream(location.toFile())) {
                            out.write(data);
                        }
                        shutdownTasks.add(new DeleteRunnable(location));
                    }

                    @Override
                    public void writeResource(String name, byte[] data) throws IOException {
                        Path location = testWiringClassesDir.resolve(name);
                        Files.createDirectories(location.getParent());
                        try (FileOutputStream out = new FileOutputStream(location.toFile())) {
                            out.write(data);
                        }
                        shutdownTasks.add(new DeleteRunnable(location));
                    }
                })
                .setTransformerTarget(new TransformerTarget() {
                    @Override
                    public void setTransformers(
                            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions) {
                        ClassLoader main = Thread.currentThread().getContextClassLoader();

                        //we need to use a temp class loader, or the old resource location will be cached
                        ClassLoader temp = new ClassLoader() {
                            @Override
                            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                                // First, check if the class has already been loaded
                                Class<?> c = findLoadedClass(name);
                                if (c == null) {
                                    c = findClass(name);
                                }
                                if (resolve) {
                                    resolveClass(c);
                                }
                                return c;
                            }

                            @Override
                            public URL getResource(String name) {
                                return main.getResource(name);
                            }

                            @Override
                            public Enumeration<URL> getResources(String name) throws IOException {
                                return main.getResources(name);
                            }
                        };
                        for (Map.Entry<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> e : functions
                                .entrySet()) {
                            String resourceName = e.getKey().replace('.', '/') + ".class";
                            try (InputStream stream = temp.getResourceAsStream(resourceName)) {
                                if (stream == null) {
                                    System.err.println("Failed to transform " + e.getKey());
                                    continue;
                                }
                                byte[] data = IoUtil.readBytes(stream);

                                ClassReader cr = new ClassReader(data);
                                ClassWriter cw = new QuarkusClassWriter(cr,
                                        ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {

                                    @Override
                                    protected ClassLoader getClassLoader() {
                                        return temp;
                                    }
                                };
                                ClassLoader old = Thread.currentThread().getContextClassLoader();
                                Thread.currentThread().setContextClassLoader(temp);
                                try {
                                    ClassVisitor visitor = cw;
                                    for (BiFunction<String, ClassVisitor, ClassVisitor> i : e.getValue()) {
                                        visitor = i.apply(e.getKey(), visitor);
                                    }
                                    cr.accept(visitor, 0);
                                } finally {
                                    Thread.currentThread().setContextClassLoader(old);
                                }

                                Path location = testWiringClassesDir.resolve(resourceName);
                                Files.createDirectories(location.getParent());
                                try (FileOutputStream out = new FileOutputStream(location.toFile())) {
                                    out.write(cw.toByteArray());
                                }
                                shutdownTasks.add(new DeleteRunnable(location));
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                })
                .addChainCustomizer(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                context.produce(new TestClassPredicateBuildItem(new Predicate<String>() {
                                    @Override
                                    public boolean test(String className) {
                                        return PathTestHelper.isTestClass(className, testClassLoader);
                                    }
                                }));
                            }
                        }).produces(TestClassPredicateBuildItem.class)
                                .build();
                    }
                })
                .addChainCustomizer(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                context.produce(new TestAnnotationBuildItem(QuarkusTest.class.getName()));
                            }
                        }).produces(TestAnnotationBuildItem.class)
                                .build();
                    }
                })
                .build();
        runtimeRunner.run();

        Closeable shutdownTask = new Closeable() {
            @Override
            public void close() throws IOException {
                runtimeRunner.close();
                while (!shutdownTasks.isEmpty()) {
                    shutdownTasks.pop().run();
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    shutdownTask.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "Quarkus Test Cleanup Shutdown task"));
        return new ExtensionState(testResourceManager, shutdownTask, false);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        restAssuredURLManager.clearURL();
        TestScopeManager.setup();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        restAssuredURLManager.setURL();
        TestScopeManager.tearDown();
    }

    @Override
    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
            throws TestInstantiationException {
        if (failedBoot) {
            try {
                return extensionContext.getRequiredTestClass().newInstance();
            } catch (Exception e) {
                throw new TestInstantiationException("Boot failed", e);
            }
        }
        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        PropertyTestUtil.setLogFileProperty();
        boolean substrateTest = extensionContext.getRequiredTestClass().isAnnotationPresent(SubstrateTest.class);
        if (state == null) {
            TestResourceManager testResourceManager = new TestResourceManager(extensionContext.getRequiredTestClass());
            try {
                Map<String, String> systemProps = testResourceManager.start();

                if (substrateTest) {
                    NativeImageLauncher launcher = new NativeImageLauncher(extensionContext.getRequiredTestClass());
                    launcher.addSystemProperties(systemProps);
                    try {
                        launcher.start();
                    } catch (IOException e) {
                        try {
                            launcher.close();
                        } catch (Throwable t) {
                        }
                        throw new JUnitException("Quarkus native image start failed, original cause: " + e);
                    }
                    state = new ExtensionState(testResourceManager, launcher, true);
                } else {
                    state = doJavaStart(extensionContext, testResourceManager);
                }
                store.put(ExtensionState.class.getName(), state);

            } catch (RuntimeException e) {
                try {
                    testResourceManager.stop();
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
                failedBoot = true;
                throw e;
            }
        } else {
            if (substrateTest != state.isSubstrate()) {
                throw new RuntimeException(
                        "Attempted to mix @SubstrateTest and JVM mode tests in the same test run. This is not allowed.");
            }
        }

        Object instance = TestInstantiator.instantiateTest(factoryContext.getTestClass());
        TestHTTPResourceManager.inject(instance);
        TestInjectionManager.inject(instance);
        return instance;
    }

    private static ClassLoader setCCL(ClassLoader cl) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        return original;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (failedBoot) {
            throw new TestAbortedException("Not running test as boot failed");
        }
    }

    class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final TestResourceManager testResourceManager;
        private final Closeable resource;
        private final boolean substrate;

        ExtensionState(TestResourceManager testResourceManager, Closeable resource, boolean substrate) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
            this.substrate = substrate;
        }

        @Override
        public void close() throws Throwable {
            testResourceManager.stop();
            try {
                resource.close();
            } finally {
                if (QuarkusTestExtension.this.originalCl != null) {
                    setCCL(QuarkusTestExtension.this.originalCl);
                }
            }
            if (appCl != null) {
                appCl.close();
            }
        }

        public boolean isSubstrate() {
            return substrate;
        }
    }

    static class DeleteRunnable implements Runnable {
        final Path path;

        DeleteRunnable(Path path) {
            this.path = path;
        }

        @Override
        public void run() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
