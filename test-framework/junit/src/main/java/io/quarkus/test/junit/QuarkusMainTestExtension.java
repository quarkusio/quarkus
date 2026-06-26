package io.quarkus.test.junit;

import static io.quarkus.deployment.dev.testing.ApplicationPropertiesUtils.createTempApplicationProperties;
import static io.quarkus.runtime.LaunchMode.TEST;
import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.TestResourceUtil.TestResourceManagerReflections.copyEntriesFromProfile;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.LogContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.deployment.dev.testing.LogCapturingOutputFilter;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.runtime.ValueRegistryImpl;
import io.quarkus.runtime.configuration.ConfigSourceOrdinal;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.value.registry.ValueRegistry;

public class QuarkusMainTestExtension extends AbstractJvmQuarkusTestExtension
        implements InvocationInterceptor, BeforeEachCallback, AfterEachCallback, ParameterResolver, BeforeAllCallback,
        AfterAllCallback, ExecutionCondition {

    PrepareResult prepareResult;
    LinkedBlockingDeque<Runnable> shutdownTasks;
    protected ClassLoader originalCl;

    /**
     * The result from an {@link Launch} test
     */
    LaunchResult result;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (isIntegrationTest(context.getRequiredTestClass())) {
            return;
        }

        ensurePrepared(context);
    }

    private void ensurePrepared(ExtensionContext extensionContext) throws Exception {
        QuarkusTestExtensionState state = getState(extensionContext);
        // we reload the test resources if we changed test class and if we had or will have per-test test resources
        boolean isNewTestClass = !Objects.equals(extensionContext.getRequiredTestClass(), currentJUnitTestClass);
        if (isWrongProfile(extensionContext) || (isNewTestClass && TestResourceUtil.testResourcesRequireReload(state,
                extensionContext.getRequiredTestClass(), getQuarkusTestProfile(extensionContext)))) {
            if (state != null) {
                try {
                    state.close();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
            prepareResult = null;
        }
        if (isNewTestClass && extensionContext.getRequiredTestClass().isAnnotationPresent(Nested.class)) {
            // we need to rerun the augmentor in this case
            prepareResult = null;
        }
        if (prepareResult == null) {
            shutdownTasks = new LinkedBlockingDeque<>();
            prepareResult = prepare(extensionContext);
        }
    }

    protected PrepareResult prepare(ExtensionContext context) throws Exception {
        Optional<Class<? extends QuarkusTestProfile>> testProfile = getQuarkusTestProfile(context);
        originalCl = Thread.currentThread().getContextClassLoader();
        quarkusTestProfile = testProfile.orElse(null);
        Class<?> requiredTestClass = context.getRequiredTestClass();

        List<Path> additionalPaths = new ArrayList<>();
        if (testProfile.isPresent()) {
            TestProfileAndProperties testProfileAndProperties = TestProfileAndProperties.of(testProfile.get(), TEST);
            additionalPaths.add(createTempApplicationProperties(
                    ConfigSourceOrdinal.TEST_PROFILE.getName(),
                    testProfileAndProperties.properties(),
                    ConfigSourceOrdinal.TEST_PROFILE));
        }

        CuratedApplication curatedApplication = AppMakerHelper.makeCuratedApplication(requiredTestClass, additionalPaths,
                context.getDisplayName(), false);
        return AppMakerHelper.prepare(requiredTestClass, curatedApplication, testProfile.map(klass -> klass));
    }

    private LaunchResult doLaunch(ExtensionContext context, String[] arguments) throws Exception {
        ensurePrepared(context);
        LogCapturingOutputFilter filter = new LogCapturingOutputFilter(prepareResult.curatedApplication(), false, false,
                () -> true);
        QuarkusConsole.addOutputFilter(filter);
        List<LogRecord> capturedRecords = new CopyOnWriteArrayList<>();
        ExtHandler logHandler = new ExtHandler() {
            @Override
            public void publish(LogRecord record) {
                capturedRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public java.util.logging.Level getLevel() {
                return Level.ALL;
            }
        };
        try {
            var exitCode = doJavaStart(context, arguments, logHandler);
            //merge all the output into one, strip ansi, then split into lines
            List<String> out = Arrays
                    .asList(String.join("", filter.captureOutput())
                            .replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "")
                            .split("\n"));
            List<String> err = Arrays
                    .asList(String.join("", filter.captureErrorOutput())
                            .replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "")
                            .split("\n"));
            List<LogRecord> logRecords = new ArrayList<>(capturedRecords);
            return new LaunchResult() {
                @Override
                public List<String> getOutputStream() {
                    return out;
                }

                @Override
                public List<String> getErrorStream() {
                    return err;
                }

                @Override
                public int exitCode() {
                    return exitCode;
                }

                @Override
                public List<LogRecord> getLogRecords() {
                    return logRecords;
                }
            };
        } finally {
            QuarkusConsole.removeOutputFilter(filter);
            Thread.currentThread()
                    .setContextClassLoader(originalCl);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        result = null;
    }

    private void flushAllLoggers() {
        Enumeration<String> loggerNames = LogContext.getLogContext()
                .getLoggerNames();
        while (loggerNames != null && loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            var logger = LogContext.getLogContext()
                    .getLogger(loggerName);
            for (Handler h : logger.getHandlers()) {
                h.flush();
            }
        }
    }

    private int doJavaStart(ExtensionContext context, String[] arguments, ExtHandler logHandler) throws Exception {
        TracingHandler.quarkusStarting();
        Closeable testResourceManager = null;
        try {
            StartupAction startupAction = prepareResult.augmentAction().createInitialRuntimeApplication();
            Thread.currentThread().setContextClassLoader(startupAction.getClassLoader());
            QuarkusConsole.installRedirects();
            flushAllLoggers();
            var rootLogger = LogContext.getLogContext().getLogger("");
            rootLogger.addHandler(logHandler);

            Class<? extends QuarkusTestProfile> profileClass = getQuarkusTestProfile(context).orElse(null);
            TestProfileAndProperties testProfileAndProperties = TestProfileAndProperties.ofNullable(profileClass, TEST);
            Optional<QuarkusTestProfile> profileInstance = testProfileAndProperties.testProfile();

            // We need to start up the resources, but we need to do it in the classloader
            // of the running application, not the classloader of the test
            // (for a main test, they are not the same)
            //must be done after the TCCL has been set
            Class<?> testResourceManagerClass = startupAction.getClassLoader().loadClass(TestResourceManager.class.getName());
            testResourceManager = TestResourceUtil.TestResourceManagerReflections.createReflectively(testResourceManagerClass,
                    context.getRequiredTestClass(),
                    profileClass,
                    copyEntriesFromProfile(profileInstance, startupAction.getClassLoader()),
                    testProfileAndProperties.isDisabledGlobalTestResources(),
                    startupAction.getOrInitialiseDevServicesProperties(),
                    Optional.ofNullable(startupAction.getOrInitialiseDevServicesNetworkId()));
            TestResourceUtil.TestResourceManagerReflections.initReflectively(testResourceManager, profileClass);
            Map<String, String> properties = TestResourceUtil.TestResourceManagerReflections
                    .startReflectively(testResourceManager);
            startupAction.overrideConfig(properties);

            testResourceManager.getClass()
                    .getMethod("inject", ValueRegistry.class, Object.class)
                    .invoke(testResourceManager, ValueRegistryImpl.builder().build(), context.getRequiredTestInstance());

            var result = startupAction.runMainClassBlocking(arguments);
            flushAllLoggers();
            return result;
        } catch (Throwable e) {
            if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                activateLogging();
            }
            throw e;
        } finally {
            try {
                if (testResourceManager != null) {
                    testResourceManager.close();
                }
            } catch (Exception e) {
                System.err.println("Unable to shutdown resource: " + e.getMessage());
            }

            var rootLogger = LogContext.getLogContext().getLogger("");
            rootLogger.removeHandler(logHandler);
            QuarkusConsole.uninstallRedirects();
            if (originalCl != null) {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        }
    }

    private static final String AESH_LAUNCHER_CLASS = "io.quarkus.test.aesh.AeshLauncher";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (isIntegrationTest(extensionContext.getRequiredTestClass())) {
            return false;
        }
        Class<?> type = parameterContext.getParameter()
                .getType();
        return type == LaunchResult.class || type == QuarkusMainLauncher.class
                || AESH_LAUNCHER_CLASS.equals(type.getName());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (type == LaunchResult.class) {
            var launch = extensionContext.getRequiredTestMethod()
                    .getAnnotation(Launch.class);
            if (launch != null) {
                doLaunchAndAssertExitCode(extensionContext, launch);
            } else {
                throw new RuntimeException(
                        "To use 'LaunchResult' as a method parameter, the test must be annotated with '@Launch'");
            }

            return result;
        } else if (type == QuarkusMainLauncher.class) {
            return createMainLauncher(extensionContext);
        } else if (AESH_LAUNCHER_CLASS.equals(type.getName())) {
            return createAeshLauncher(extensionContext);
        } else {
            throw new RuntimeException("Parameter type not supported");
        }
    }

    private QuarkusMainLauncher createMainLauncher(ExtensionContext extensionContext) {
        return new QuarkusMainLauncher() {
            @Override
            public LaunchResult launch(String... args) {
                try {
                    return doLaunch(extensionContext, args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Creates an AeshLauncher instance via reflection to avoid a compile-time
     * dependency on quarkus-test-aesh.
     */
    private Object createAeshLauncher(ExtensionContext extensionContext) {
        try {
            QuarkusMainLauncher mainLauncher = createMainLauncher(extensionContext);
            Class<?> implClass = Thread.currentThread().getContextClassLoader()
                    .loadClass("io.quarkus.test.aesh.AeshLauncherImpl");
            return implClass.getDeclaredConstructor(QuarkusMainLauncher.class).newInstance(mainLauncher);
        } catch (ClassNotFoundException e) {
            throw new ParameterResolutionException(
                    "AeshLauncher requested but quarkus-test-aesh is not on the classpath. "
                            + "Add io.quarkus:quarkus-test-aesh as a test dependency.",
                    e);
        } catch (ReflectiveOperationException e) {
            throw new ParameterResolutionException("Failed to create AeshLauncher", e);
        }
    }

    private void doLaunchAndAssertExitCode(ExtensionContext extensionContext, Launch launch) {
        // HACK: we launch the application in this method instead of in beforeEach in order to ensure that
        // tests that use @BeforeEach will have a chance to run before the application is launched

        String[] arguments = launch.value();
        LaunchResult r;
        try {
            r = doLaunch(extensionContext, arguments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assertions.assertEquals(launch.exitCode(), r.exitCode(),
                "Exit code did not match, output: " + r.getOutput() + " " + r.getErrorOutput());
        this.result = r;
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (invocationContext.getArguments()
                .isEmpty()) {
            var launch = extensionContext.getRequiredTestMethod()
                    .getAnnotation(Launch.class);
            if (launch != null) {
                // in this case, resolveParameter has not been called by JUnit, so we need to make sure the application is launched
                doLaunchAndAssertExitCode(extensionContext, launch);
            }
        }

        invocation.proceed();
    }

    private boolean isIntegrationTest(Class<?> clazz) {
        for (Class<?> i : currentTestClassStack) {
            if (i.isAnnotationPresent(QuarkusMainIntegrationTest.class)) {
                return true;
            }
        }
        if (clazz.isAnnotationPresent(QuarkusMainIntegrationTest.class)) {
            return true;
        }
        return false;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        currentTestClassStack.pop();

        try {
            if (shutdownTasks != null) {
                for (Runnable shutdownTask : shutdownTasks) {
                    shutdownTask.run();
                }
            }
            shutdownTasks = null;
        } catch (Exception e) {
            System.err.println("Unable to run shutdown tasks: " + e.getMessage());
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        currentTestClassStack.push(context.getRequiredTestClass());
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return super.evaluateExecutionCondition(context);
    }
}
