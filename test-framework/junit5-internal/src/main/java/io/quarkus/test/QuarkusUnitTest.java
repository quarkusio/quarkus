package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.jboss.logmanager.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestInstantiationException;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildException;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runner.bootstrap.AugmentActionImpl;
import io.quarkus.runner.bootstrap.StartupActionImpl;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.runtime.logging.JBossVersion;
import io.quarkus.test.common.GroovyClassValue;
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
        InvocationInterceptor, ParameterResolver {

    public static final String THE_BUILD_WAS_EXPECTED_TO_FAIL = "The build was expected to fail";
    private static final String APP_ROOT = "app-root";

    private static final Logger rootLogger;
    private Handler[] originalHandlers;

    static {
        JBossVersion.disableVersionLogging();
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        rootLogger = (Logger) LogManager.getLogManager().getLogger("");
    }

    boolean started = false;

    private Path deploymentDir;
    private Consumer<Throwable> assertException;
    private Supplier<JavaArchive> archiveProducer;
    private List<JavaArchive> additionalDependencies = new ArrayList<>();

    private List<Consumer<BuildChainBuilder>> buildChainCustomizers = new ArrayList<>();
    private Runnable afterUndeployListener;

    private String logFileName;
    private InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler((r) -> false);
    private Consumer<List<LogRecord>> assertLogRecords;

    private Timer timeoutTimer;
    private volatile TimerTask timeoutTask;
    private Properties customApplicationProperties;
    private Map<String, String> customRuntimeApplicationProperties;
    private Runnable beforeAllCustomizer;
    private Runnable afterAllCustomizer;
    private CuratedApplication curatedApplication;
    private RunningQuarkusApplication runningQuarkusApplication;
    private ClassLoader originalClassLoader;
    private List<Dependency> forcedDependencies = Collections.emptyList();

    private boolean useSecureConnection;

    private Class<?> actualTestClass;
    private Object actualTestInstance;
    private String[] commandLineParameters = new String[0];

    private boolean allowTestClassOutsideDeployment;
    private boolean flatClassPath;
    private List<ClassLoaderEventListener> classLoadListeners = new ArrayList<>();

    private List<Object> testMethodInvokers;

    private List<Consumer<QuarkusBootstrap.Builder>> bootstrapCustomizers = new ArrayList<>();

    public QuarkusUnitTest setExpectedException(Class<? extends Throwable> expectedException) {
        return setExpectedException(expectedException, false);
    }

    public QuarkusUnitTest setExpectedException(Class<? extends Throwable> expectedException, boolean logMessage) {
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
            if (found && logMessage) {
                System.out.println("Build failed with the expected exception:" + i);
            }
            assertTrue(found, "Build failed with a wrong exception, expected " + expectedException + " but got " + t);
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

    /**
     *
     * @param archiveProducer
     * @return self
     * @see #withApplicationRoot(Consumer)
     */
    public QuarkusUnitTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        this.archiveProducer = Objects.requireNonNull(archiveProducer);
        return this;
    }

    /**
     * Customize the application root.
     *
     * @param applicationRootConsumer
     * @return self
     */
    public QuarkusUnitTest withApplicationRoot(Consumer<JavaArchive> applicationRootConsumer) {
        Objects.requireNonNull(applicationRootConsumer);
        return setArchiveProducer(() -> {
            JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
            applicationRootConsumer.accept(jar);
            return jar;
        });
    }

    /**
     * Use an empty application for the test
     *
     * @return self
     */
    public QuarkusUnitTest withEmptyApplication() {
        return withApplicationRoot(new Consumer<JavaArchive>() {
            @Override
            public void accept(JavaArchive javaArchive) {

            }
        });
    }

    /**
     * Add the java archive as an additional dependency. This dependency is always considered an application archive, even if it
     * would not otherwise be one.
     *
     * @param archive
     * @return self
     */
    public QuarkusUnitTest addAdditionalDependency(JavaArchive archive) {
        this.additionalDependencies.add(Objects.requireNonNull(archive));
        return this;
    }

    /**
     * Add the java archive as an additional dependency. This dependency is always considered an application archive, even if it
     * would not otherwise be one.
     *
     * @param dependencyConsumer
     * @return self
     */
    public QuarkusUnitTest withAdditionalDependency(Consumer<JavaArchive> dependencyConsumer) {
        JavaArchive dependency = ShrinkWrap.create(JavaArchive.class);
        Objects.requireNonNull(dependencyConsumer).accept(dependency);
        return addAdditionalDependency(dependency);
    }

    public QuarkusUnitTest addBuildChainCustomizer(Consumer<BuildChainBuilder> customizer) {
        this.buildChainCustomizers.add(customizer);
        return this;
    }

    public QuarkusUnitTest addClassLoaderEventListener(ClassLoaderEventListener listener) {
        this.classLoadListeners.add(listener);
        return this;
    }

    public QuarkusUnitTest setLogFileName(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }

    public QuarkusUnitTest setLogRecordPredicate(Predicate<LogRecord> predicate) {
        this.inMemoryLogHandler = new InMemoryLogHandler(predicate);
        return this;
    }

    /**
     * If this test should use a single ClassLoader to load all the classes.
     *
     * This is sometimes necessary when testing Quarkus itself, and we want the test classes
     * and Quarkus classes to be in the same CL.
     *
     */
    public QuarkusUnitTest setFlatClassPath(boolean flatClassPath) {
        this.flatClassPath = flatClassPath;
        return this;
    }

    public QuarkusUnitTest assertLogRecords(Consumer<List<LogRecord>> assertLogRecords) {
        if (this.assertLogRecords != null) {
            throw new IllegalStateException("Don't set the a log record assertion twice"
                    + " to avoid shadowing out the first call.");
        }
        this.assertLogRecords = assertLogRecords;
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
    public QuarkusUnitTest setForcedDependencies(List<Dependency> forcedDependencies) {
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

    /**
     * An advanced option that allows tests to customize the {@link QuarkusBootstrap.Builder} that will be used to create the
     * {@link CuratedApplication}
     */
    public QuarkusUnitTest addBootstrapCustomizer(Consumer<QuarkusBootstrap.Builder> consumer) {
        this.bootstrapCustomizers.add(consumer);
        return this;
    }

    private void exportArchives(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = getArchiveProducerOrDefault();
            Class<?> c = testClass;
            archive.addClasses(c.getClasses());
            while (c != Object.class) {
                archive.addClass(c);
                c = c.getSuperclass();
            }
            if (customApplicationProperties != null) {
                archive.add(new PropertiesAsset(customApplicationProperties), "application.properties");
            }
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.resolve(APP_ROOT).toFile());

            for (JavaArchive dependency : additionalDependencies) {
                dependency.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.resolve(dependency.getName()).toFile());
            }

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
        runExtensionMethod(invocationContext, extensionContext, false);
        invocation.skip();
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        runExtensionMethod(invocationContext, extensionContext, true);
        invocation.skip();
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext, extensionContext, true);
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext, extensionContext, false);
        }
        invocation.skip();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext, extensionContext, true);
        }
        invocation.skip();
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (assertException == null) {
            runExtensionMethod(invocationContext, extensionContext, false);
        }
        invocation.skip();
    }

    private void runExtensionMethod(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
            boolean testMethodInvokersAllowed) throws Throwable {
        Method newMethod = null;
        Class<?> c = actualTestClass;
        while ((c != Object.class) && newMethod == null) {
            Method[] declaredMethods = c.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                if (!declaredMethod.getName().equals(invocationContext.getExecutable().getName())) {
                    continue;
                }
                boolean parametersMatch = true;
                for (Class<?> declaredMethodParameterType : declaredMethod.getParameterTypes()) {
                    boolean parameterTypeFound = false;
                    for (Class<?> executionContextParameterType : invocationContext.getExecutable().getParameterTypes()) {
                        if (declaredMethodParameterType.getName().equals(executionContextParameterType.getName())) {
                            parameterTypeFound = true;
                            break;
                        }
                    }
                    if (!parameterTypeFound) {
                        parametersMatch = false;
                        break;
                    }
                }
                if (parametersMatch) {
                    newMethod = declaredMethod;
                    break;
                }
            }
            c = c.getSuperclass();
        }
        if (newMethod == null) {
            throw new RuntimeException("Could not find method " + invocationContext.getExecutable() + " on test class");
        }

        Object testMethodInvokerToUse = null;
        if (testMethodInvokersAllowed) {
            for (Object testMethodInvoker : testMethodInvokers) {
                boolean supportsMethod = (boolean) testMethodInvoker.getClass()
                        .getMethod("supportsMethod", Class.class, Method.class).invoke(testMethodInvoker,
                                extensionContext.getRequiredTestClass(), invocationContext.getExecutable());
                if (supportsMethod) {
                    testMethodInvokerToUse = testMethodInvoker;
                    break;
                }
            }
        }

        try {
            newMethod.setAccessible(true);
            if (testMethodInvokerToUse != null) {
                List<Object> effectiveArguments = new ArrayList<>(invocationContext.getArguments().size());
                Class<?>[] parameterTypes = newMethod.getParameterTypes();
                if (parameterTypes.length != invocationContext.getArguments().size()) {
                    throw new IllegalStateException(
                            "Improper integration of '" + testMethodInvokerToUse.getClass() + "' detected");
                }
                for (int i = 0; i < invocationContext.getArguments().size(); i++) {
                    Object originalValue = invocationContext.getArguments().get(i);
                    if ((originalValue == null)
                            && (testMethodInvokerHandlesParamType(testMethodInvokerToUse, parameterTypes[i].getName()))) {
                        effectiveArguments
                                .add(testMethodInvokerParamInstance(testMethodInvokerToUse, parameterTypes[i].getName()));
                    } else {
                        effectiveArguments.add(originalValue);
                    }
                }
                testMethodInvokerToUse.getClass()
                        .getMethod("invoke", Object.class, Method.class, List.class, String.class)
                        .invoke(testMethodInvokerToUse, actualTestInstance, newMethod, effectiveArguments,
                                extensionContext.getRequiredTestClass().getName());
            } else {
                newMethod.invoke(actualTestInstance, invocationContext.getArguments().toArray());
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        GroovyClassValue.disable();
        //set the right launch mode in the outer CL, used by the HTTP host config source
        ProfileManager.setLaunchMode(LaunchMode.TEST);
        if (beforeAllCustomizer != null) {
            beforeAllCustomizer.run();
        }
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        originalHandlers = rootLogger.getHandlers();
        rootLogger.addHandler(inMemoryLogHandler);

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
        if (logFileName != null) {
            PropertyTestUtil.setLogFileProperty(logFileName);
        } else {
            PropertyTestUtil.setLogFileProperty();
        }
        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);

        ExclusivityChecker.checkTestType(extensionContext, QuarkusUnitTest.class);

        TestResourceManager testResourceManager = (TestResourceManager) store.get(TestResourceManager.class.getName());
        if (testResourceManager == null) {
            testResourceManager = new TestResourceManager(extensionContext.getRequiredTestClass());
            testResourceManager.init(null);
            testResourceManager.start();
            TestResourceManager tm = testResourceManager;
            store.put(TestResourceManager.class.getName(), testResourceManager);
            store.put(TestResourceManager.CLOSEABLE_NAME, new ExtensionContext.Store.CloseableResource() {

                @Override
                public void close() throws Throwable {
                    tm.close();
                }
            });
        }

        Class<?> testClass = extensionContext.getRequiredTestClass();

        try {
            deploymentDir = Files.createTempDirectory("quarkus-unit-test");

            exportArchives(deploymentDir, testClass);

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

                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                //we need to make sure all hot reloadable classes are application classes
                                context.produce(new ApplicationClassPredicateBuildItem(new Predicate<String>() {
                                    @Override
                                    public boolean test(String s) {
                                        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread()
                                                .getContextClassLoader();
                                        //if the class file is present in this (and not the parent) CL then it is an application class
                                        List<ClassPathElement> res = cl
                                                .getElementsWithResource(s.replace(".", "/") + ".class", true);
                                        return !res.isEmpty();
                                    }
                                }));
                            }
                        }).produces(ApplicationClassPredicateBuildItem.class).build();
                    }
                });
            } catch (ClassNotFoundException e) {
                System.err.println("Couldn't make the test class " + testClass.getSimpleName() + " an unremovable bean"
                        + " (probably because a dependency on io.quarkus:quarkus-arc-deployment is missing);"
                        + " other beans may also be removed and injection may not work as expected");
            }

            try {
                final Path testLocation = PathTestHelper.getTestClassesLocation(testClass);
                final Path projectDir = Path.of("").normalize().toAbsolutePath();
                QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                        .setApplicationRoot(deploymentDir.resolve(APP_ROOT))
                        .setMode(QuarkusBootstrap.Mode.TEST)
                        .addExcludedPath(testLocation)
                        .setProjectRoot(projectDir)
                        .setTargetDirectory(PathTestHelper.getProjectBuildDir(projectDir, testLocation))
                        .setFlatClassPath(flatClassPath)
                        .setForcedDependencies(forcedDependencies);
                for (JavaArchive dependency : additionalDependencies) {
                    builder.addAdditionalApplicationArchive(
                            new AdditionalDependency(deploymentDir.resolve(dependency.getName()), false, true));
                }
                if (!forcedDependencies.isEmpty()) {
                    //if we have forced dependencies we can't use the cache
                    //as it can screw everything up
                    builder.setDisableClasspathCache(true);
                }
                if (!allowTestClassOutsideDeployment) {
                    builder
                            .setBaseClassLoader(
                                    QuarkusClassLoader
                                            .builder("QuarkusUnitTest ClassLoader", getClass().getClassLoader(), false)
                                            .addClassLoaderEventListeners(this.classLoadListeners)
                                            .addBannedElement(ClassPathElement.fromPath(testLocation, true)).build());
                }
                builder.addClassLoaderEventListeners(this.classLoadListeners);

                for (Consumer<QuarkusBootstrap.Builder> bootstrapCustomizer : bootstrapCustomizers) {
                    bootstrapCustomizer.accept(builder);
                }
                curatedApplication = builder.build().bootstrap();

                StartupActionImpl startupAction = new AugmentActionImpl(curatedApplication, customizers, classLoadListeners)
                        .createInitialRuntimeApplication();
                Map<String, String> overriddenConfig = new HashMap<>(testResourceManager.getConfigProperties());
                if (customRuntimeApplicationProperties != null) {
                    overriddenConfig.putAll(customRuntimeApplicationProperties);
                }
                startupAction.overrideConfig(overriddenConfig);
                runningQuarkusApplication = startupAction
                        .run(commandLineParameters);
                //we restore the CL at the end of the test
                Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());
                if (assertException != null) {
                    fail(THE_BUILD_WAS_EXPECTED_TO_FAIL);
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

                    populateTestMethodInvokers(startupAction.getClassLoader());

                } catch (Exception e) {
                    throw new TestInstantiationException("Failed to create test instance", e);
                }

                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(testClass.getName(), actualTestInstance);
            } catch (Throwable e) {
                started = false;
                if (assertException != null) {
                    if (e instanceof AssertionError && e.getMessage().equals(THE_BUILD_WAS_EXPECTED_TO_FAIL)) {
                        //don't pass the 'no failure' assertion into the assert exception function
                        throw e;
                    }
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
        actualTestClass = null;
        actualTestInstance = null;
        List<LogRecord> records = null;
        if (assertLogRecords != null) {
            records = new ArrayList<>(inMemoryLogHandler.records);
        }
        rootLogger.setHandlers(originalHandlers);
        inMemoryLogHandler.clearRecords();
        inMemoryLogHandler.setFilter(null);

        try {
            if (runningQuarkusApplication != null) {
                runningQuarkusApplication.close();
                runningQuarkusApplication = null;
            }
            if (afterUndeployListener != null) {
                afterUndeployListener.run();
            }
            if (curatedApplication != null) {
                curatedApplication.close();
                curatedApplication = null;
            }
        } finally {
            System.clearProperty("test.url");
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            originalClassLoader = null;
            timeoutTask.cancel();
            timeoutTask = null;
            timeoutTimer.cancel();
            timeoutTimer = null;
            if (deploymentDir != null) {
                FileUtil.deleteDirectory(deploymentDir);
            }

            if (afterAllCustomizer != null) {
                afterAllCustomizer.run();
            }
            ClearCache.clearAnnotationCache();
        }
        if (records != null) {
            assertLogRecords.accept(records);
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
            URL systemResource = ClassLoader.getSystemResource(resourceName);
            if (systemResource == null) {
                throw new FileNotFoundException("Resource '" + resourceName + "' not found");
            }
            try (InputStream in = systemResource.openStream()) {
                customApplicationProperties.load(in);
            }
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load resource: '" + resourceName + "'", e);
        }
    }

    public QuarkusUnitTest overrideConfigKey(final String propertyKey, final String propertyValue) {
        if (customApplicationProperties == null) {
            customApplicationProperties = new Properties();
        }
        customApplicationProperties.put(propertyKey, propertyValue);
        return this;
    }

    public QuarkusUnitTest overrideRuntimeConfigKey(final String propertyKey, final String propertyValue) {
        if (customRuntimeApplicationProperties == null) {
            customRuntimeApplicationProperties = new HashMap<>();
        }
        customRuntimeApplicationProperties.put(propertyKey, propertyValue);
        return this;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        boolean isConstructor = parameterContext.getDeclaringExecutable() instanceof Constructor;
        if (isConstructor) {
            return true;
        }
        if (!(parameterContext.getDeclaringExecutable() instanceof Method)) {
            return false;
        }
        if (testMethodInvokers == null) {
            return false;
        }
        for (Object testMethodInvoker : testMethodInvokers) {
            boolean handlesMethodParamType = testMethodInvokerHandlesParamType(testMethodInvoker,
                    parameterContext.getParameter().getType().getName());
            if (handlesMethodParamType) {
                return true;
            }
        }
        return false;
    }

    /**
     * We don't actually have to resolve the parameter (thus the default values in the implementation)
     * since the class instance that is passed to JUnit isn't really used.
     * The actual test instance that is used is the one that is pulled from Arc, which of course will already have its
     * constructor parameters properly resolved
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if ((parameterContext.getDeclaringExecutable() instanceof Method) && (testMethodInvokers != null)) {
            for (Object testMethodInvoker : testMethodInvokers) {
                if (testMethodInvokerHandlesParamType(testMethodInvoker,
                        parameterContext.getParameter().getType().getName())) {
                    return null; // don't return the actual value since it leads to class loading issues
                }
            }
        }
        String className = parameterContext.getParameter().getType().getName();
        switch (className) {
            case "boolean":
                return false;
            case "byte":
            case "short":
            case "int":
                return 0;
            case "long":
                return 0L;
            case "float":
                return 0.0f;
            case "double":
                return 0.0d;
            case "char":
                return '\u0000';
            default:
                return null;
        }
    }

    // we need to use reflection because the instances of TestMethodInvoker are loaded from the QuarkusClassLoader
    private boolean testMethodInvokerHandlesParamType(Object testMethodInvoker, String parameterTypeName) {
        try {
            return (boolean) testMethodInvoker.getClass().getMethod("handlesMethodParamType", String.class)
                    .invoke(testMethodInvoker, parameterTypeName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to determine if TestMethodInvoker supports parameter", e);
        }
    }

    // we need to use reflection because the instances of TestMethodInvoker are loaded from the QuarkusClassLoader
    private Object testMethodInvokerParamInstance(Object testMethodInvoker, String parameterTypeName) {
        try {
            return testMethodInvoker.getClass().getMethod("methodParamInstance", String.class)
                    .invoke(testMethodInvoker, parameterTypeName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to obtain instance of parameter using TestMethodInvoker", e);
        }
    }

    private void populateTestMethodInvokers(ClassLoader quarkusClassLoader) {
        testMethodInvokers = new ArrayList<>();
        try {
            ServiceLoader<?> loader = ServiceLoader.load(quarkusClassLoader.loadClass(TestMethodInvoker.class.getName()),
                    quarkusClassLoader);
            for (Object testMethodInvoker : loader) {
                testMethodInvokers.add(testMethodInvoker);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
