package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestInstantiationException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildException;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runner.bootstrap.AugmentActionImpl;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

/**
 * A test extension for testing Quarkus internals, not intended for end user consumption
 */
public class QuarkusUnitTest
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback,
        InvocationInterceptor {

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    boolean started = false;

    private Path deploymentDir;
    private Consumer<Throwable> assertException;
    private Supplier<JavaArchive> archiveProducer;
    private List<Consumer<BuildChainBuilder>> buildChainCustomizers = new ArrayList<>();
    private Runnable afterUndeployListener;
    private String logFileName;

    private static final Timer timeoutTimer = new Timer("Test thread dump timer");
    private volatile TimerTask timeoutTask;
    private Properties customApplicationProperties;
    private Runnable beforeAllCustomizer;
    private Runnable afterAllCustomizer;
    private CuratedApplication curatedApplication;
    private RunningQuarkusApplication runningQuarkusApplication;
    private ClassLoader originalClassLoader;
    private List<AppArtifact> forcedDependencies = Collections.emptyList();

    private boolean useSecureConnection;

    private Class<?> actualTestClass;
    private Object actualTestInstance;
    private String[] commandLineParameters = new String[0];

    private boolean allowTestClassOutsideDeployment;

    public QuarkusUnitTest setExpectedException(Class<? extends Throwable> expectedException) {
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

    public QuarkusUnitTest() {
        this(false);
    }

    public static QuarkusUnitTest withSecuredConnection() {
        return new QuarkusUnitTest(true);
    }

    private QuarkusUnitTest(boolean useSecureConnection) {
        this.useSecureConnection = useSecureConnection;
    }

    public QuarkusUnitTest assertException(Consumer<Throwable> assertException) {
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

    // set a Runnable that will run before ANYTHING else is done
    public QuarkusUnitTest setBeforeAllCustomizer(Runnable beforeAllCustomizer) {
        this.beforeAllCustomizer = beforeAllCustomizer;
        return this;
    }

    // set a Runnable that will run after EVERYTHING else is done
    public QuarkusUnitTest setAfterAllCustomizer(Runnable afterAllCustomizer) {
        this.afterAllCustomizer = afterAllCustomizer;
        return this;
    }

    /**
     * Provides a convenient way to either add additional dependencies to the application (if it doesn't already contain a
     * dependency), or override a version (if the dependency already exists)
     */
    public QuarkusUnitTest setForcedDependencies(List<AppArtifact> forcedDependencies) {
        this.forcedDependencies = forcedDependencies;
        return this;
    }

    public String[] getCommandLineParameters() {
        return commandLineParameters;
    }

    public QuarkusUnitTest setCommandLineParameters(String... commandLineParameters) {
        this.commandLineParameters = commandLineParameters;
        return this;
    }

    /**
     * Normally access to any test classes that are not packaged in the deployment will result
     * in a ClassNotFoundException. If this is true then access is allowed, which can be useful
     * when testing shutdown behaviour.
     */
    public QuarkusUnitTest setAllowTestClassOutsideDeployment(boolean allowTestClassOutsideDeployment) {
        this.allowTestClassOutsideDeployment = allowTestClassOutsideDeployment;
        return this;
    }

    private void exportArchive(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = getArchiveProducerOrDefault();
            Class<?> c = testClass;
            while (c != Object.class) {
                archive.addClass(c);
                c = c.getSuperclass();
            }
            if (customApplicationProperties != null) {
                archive.add(new PropertiesAsset(customApplicationProperties), "application.properties");
            }
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

            //debugging code
            ExportUtil.exportToQuarkusDeploymentPath(archive);
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
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        runExtensionMethod(invocationContext);
        invocation.skip();
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        runExtensionMethod(invocationContext);
        invocation.skip();
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext);
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext);
        }
        invocation.skip();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext);
        }
        invocation.skip();
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext);
        }
        invocation.skip();
    }

    private void runExtensionMethod(ReflectiveInvocationContext<Method> invocationContext) throws Throwable {
        Method newMethod = null;
        Class<?> c = actualTestClass;
        while (c != Object.class) {
            try {
                newMethod = c.getDeclaredMethod(invocationContext.getExecutable().getName(),
                        invocationContext.getExecutable().getParameterTypes());
                break;
            } catch (NoSuchMethodException e) {
                //ignore
            }
            c = c.getSuperclass();
        }
        if (newMethod == null) {
            throw new RuntimeException("Could not find method " + invocationContext.getExecutable() + " on test class");
        }
        try {
            newMethod.setAccessible(true);
            newMethod.invoke(actualTestInstance, invocationContext.getArguments().toArray());
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (beforeAllCustomizer != null) {
            beforeAllCustomizer.run();
        }
        originalClassLoader = Thread.currentThread().getContextClassLoader();
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
                    manager.close();
                }
            });
        }

        Class<?> testClass = extensionContext.getRequiredTestClass();

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

            try {
                QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                        .setApplicationRoot(deploymentDir)
                        .setMode(QuarkusBootstrap.Mode.TEST)
                        .addExcludedPath(testLocation)
                        .setProjectRoot(testLocation)
                        .setForcedDependencies(forcedDependencies.stream().map(d -> new AppDependency(d, "compile"))
                                .collect(Collectors.toList()));
                if (!allowTestClassOutsideDeployment) {
                    builder
                            .setBaseClassLoader(
                                    QuarkusClassLoader
                                            .builder("QuarkusUnitTest ClassLoader", getClass().getClassLoader(), false)
                                            .addBannedElement(ClassPathElement.fromPath(testLocation)).build());
                }
                curatedApplication = builder.build().bootstrap();

                runningQuarkusApplication = new AugmentActionImpl(curatedApplication, customizers)
                        .createInitialRuntimeApplication()
                        .run(commandLineParameters);
                //we restore the CL at the end of the test
                Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());
                if (assertException != null) {
                    fail("The build was expected to fail");
                }
                started = true;
                System.setProperty("test.url", TestHTTPResourceManager.getUri(runningQuarkusApplication));
                try {
                    actualTestClass = Class.forName(testClass.getName(), true,
                            Thread.currentThread().getContextClassLoader());
                    actualTestInstance = runningQuarkusApplication.instance(actualTestClass);
                    Class<?> resM = runningQuarkusApplication.getClassLoader()
                            .loadClass(TestHTTPResourceManager.class.getName());
                    resM.getDeclaredMethod("inject", Object.class).invoke(null, actualTestInstance);
                } catch (Exception e) {
                    throw new TestInstantiationException("Failed to create test instance", e);
                }

                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(testClass.getName(), actualTestInstance);
            } catch (Throwable e) {
                started = false;
                if (assertException != null) {
                    if (e instanceof RuntimeException) {
                        Throwable cause = e.getCause();
                        if (cause != null && cause instanceof BuildException) {
                            assertException.accept(unwrapException(cause.getCause()));
                        } else if (cause != null) {
                            assertException.accept(unwrapException(cause));
                        } else {
                            assertException.accept(e);
                        }
                    } else {
                        assertException.accept(e);
                    }
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Throwable unwrapException(Throwable cause) {
        //TODO: huge hack
        try {
            Class<?> localVer = QuarkusUnitTest.class.getClassLoader().loadClass(cause.getClass().getName());
            if (localVer != cause.getClass()) {
                Constructor<?> ctor = localVer.getConstructor(String.class, Throwable.class);
                return (Throwable) ctor.newInstance(cause.getMessage(), cause.getCause());
            }
        } catch (Exception e) {
            //failed to unwrap
        }
        return cause;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        try {
            if (runningQuarkusApplication != null) {
                runningQuarkusApplication.close();
            }
            if (afterUndeployListener != null) {
                afterUndeployListener.run();
            }
            curatedApplication.close();
        } finally {
            System.clearProperty("test.url");
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            timeoutTask.cancel();
            timeoutTask = null;
            if (deploymentDir != null) {
                FileUtil.deleteDirectory(deploymentDir);
            }
        }
        if (afterAllCustomizer != null) {
            afterAllCustomizer.run();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (runningQuarkusApplication != null) {
            //this kinda sucks, but everything is isolated, so we need to hook into everything via reflection
            runningQuarkusApplication.getClassLoader().loadClass(RestAssuredURLManager.class.getName())
                    .getDeclaredMethod("clearURL")
                    .invoke(null);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (assertException != null) {
            // Build failed as expected - test methods are not invoked
            return;
        }
        if (runningQuarkusApplication != null) {
            runningQuarkusApplication.getClassLoader().loadClass(RestAssuredURLManager.class.getName())
                    .getDeclaredMethod("setURL", boolean.class).invoke(null, useSecureConnection);
        } else {
            Optional<Class<?>> testClass = context.getTestClass();
            if (testClass.isPresent()) {
                Field extensionField = Arrays.stream(testClass.get().getDeclaredFields()).filter(
                        f -> f.isAnnotationPresent(RegisterExtension.class) && QuarkusUnitTest.class.equals(f.getType()))
                        .findAny().orElse(null);
                if (extensionField != null && !Modifier.isStatic(extensionField.getModifiers())) {
                    throw new IllegalStateException(
                            "Test application not started - QuarkusUnitTest must be used with a static field: "
                                    + extensionField);
                }
            }
            throw new IllegalStateException("Test application not started for an unknown reason");
        }
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
}
