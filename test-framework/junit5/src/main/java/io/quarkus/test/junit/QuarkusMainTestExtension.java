package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.TestResourceUtil.TestResourceManagerReflections.copyEntriesFromProfile;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Handler;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.handlers.OutputStreamHandler;
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

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.deployment.dev.testing.LogCapturingOutputFilter;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.runtime.logging.JBossVersion;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;

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

        Class<? extends QuarkusTestProfile> profile = getQuarkusTestProfile(context);
        ensurePrepared(context, profile);
    }

    private void ensurePrepared(ExtensionContext extensionContext, Class<? extends QuarkusTestProfile> profile)
            throws Exception {
        JBossVersion.disableVersionLogging();
        QuarkusTestExtensionState state = getState(extensionContext);
        boolean wrongProfile = !Objects.equals(profile, quarkusTestProfile);
        // we reload the test resources if we changed test class and if we had or will have per-test test resources
        boolean isNewTestClass = !Objects.equals(extensionContext.getRequiredTestClass(), currentJUnitTestClass);
        if (wrongProfile || (isNewTestClass
                && TestResourceUtil.testResourcesRequireReload(state, extensionContext.getRequiredTestClass(),
                        profile))) {
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
            prepareResult = prepare(extensionContext, profile, shutdownTasks);
        }
    }

    protected PrepareResult prepare(ExtensionContext context, Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws Exception {

        originalCl = Thread.currentThread().getContextClassLoader();
        quarkusTestProfile = profile;
        final Class<?> requiredTestClass = context.getRequiredTestClass();
        final CuratedApplication curatedApplication = getCuratedApplication(requiredTestClass, context.getDisplayName());

        var result = AppMakerHelper.prepare(requiredTestClass, curatedApplication, profile);
        if (result.profileInstance() != null) {
            Runnable configCleaner = AppMakerHelper.setExtraPropertiesRestorably(profile, result.profileInstance());
            shutdownTasks.add(configCleaner);
        }
        return result;
    }

    // Override, because in main tests the quarkus classloader isn't used for tests
    protected CuratedApplication getCuratedApplication(Class<?> requiredTestClass, String displayName)
            throws AppModelResolverException, IOException, BootstrapException {
        var curatedApp = AppMakerHelper.makeCuratedApplication(requiredTestClass, displayName, false);
        shutdownTasks.add(curatedApp::close);
        return curatedApp;
    }

    private LaunchResult doLaunch(ExtensionContext context, Class<? extends QuarkusTestProfile> selectedProfile,
            String[] arguments) throws Exception {
        ensurePrepared(context, selectedProfile);
        LogCapturingOutputFilter filter = new LogCapturingOutputFilter(prepareResult.curatedApplication(), false, false,
                () -> true);
        QuarkusConsole.addOutputFilter(filter);
        try {
            var result = doJavaStart(context, selectedProfile, arguments);
            //merge all the output into one, strip ansi, then split into lines
            List<String> out = Arrays
                    .asList(String.join("", filter.captureOutput())
                            .replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "")
                            .split("\n"));
            List<String> err = Arrays
                    .asList(String.join("", filter.captureErrorOutput())
                            .replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "")
                            .split("\n"));
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
                    return result;
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

    private static Handler ORIGINAL_QUARKUS_CONSOLE_HANDLER = null;
    private static Handler REDIRECT_QUARKUS_CONSOLE_HANDLER = null;

    private static void installLoggerRedirect() throws Exception {
        var rootLogger = LogContext.getLogContext()
                .getLogger("");

        ORIGINAL_QUARKUS_CONSOLE_HANDLER = null;
        REDIRECT_QUARKUS_CONSOLE_HANDLER = null;

        for (var topLevelHandler : rootLogger.getHandlers()) {
            if (topLevelHandler instanceof QuarkusDelayedHandler) {
                ORIGINAL_QUARKUS_CONSOLE_HANDLER = topLevelHandler;
                for (var h : ((QuarkusDelayedHandler) topLevelHandler).getHandlers()) {
                    if (h instanceof org.jboss.logmanager.handlers.ConsoleHandler) {
                        REDIRECT_QUARKUS_CONSOLE_HANDLER = new OutputStreamHandler(QuarkusConsole.REDIRECT_OUT,
                                h.getFormatter());
                        break;
                    }
                }
                break;
            }
        }

        if (REDIRECT_QUARKUS_CONSOLE_HANDLER != null) {
            rootLogger.removeHandler(ORIGINAL_QUARKUS_CONSOLE_HANDLER);
            rootLogger.addHandler(REDIRECT_QUARKUS_CONSOLE_HANDLER);
        }
    }

    private static void uninstallLoggerRedirect() throws Exception {
        var rootLogger = LogContext.getLogContext()
                .getLogger("");
        if (REDIRECT_QUARKUS_CONSOLE_HANDLER != null) {
            rootLogger.addHandler(ORIGINAL_QUARKUS_CONSOLE_HANDLER);
            rootLogger.removeHandler(REDIRECT_QUARKUS_CONSOLE_HANDLER);
        }
    }

    private void flushAllLoggers() {
        Enumeration<String> loggerNames = org.jboss.logmanager.LogContext.getLogContext()
                .getLoggerNames();
        while (loggerNames != null && loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            var logger = org.jboss.logmanager.LogContext.getLogContext()
                    .getLogger(loggerName);
            for (Handler h : logger.getHandlers()) {
                h.flush();
            }
        }
    }

    private int doJavaStart(ExtensionContext context, Class<? extends QuarkusTestProfile> profile, String[] arguments)
            throws Exception {
        JBossVersion.disableVersionLogging();

        TracingHandler.quarkusStarting();
        Closeable testResourceManager = null;
        try {
            StartupAction startupAction = prepareResult.augmentAction().createInitialRuntimeApplication();
            Thread.currentThread()
                    .setContextClassLoader(startupAction.getClassLoader());
            QuarkusConsole.installRedirects();
            flushAllLoggers();
            installLoggerRedirect();

            QuarkusTestProfile profileInstance = prepareResult.profileInstance();

            // We need to start up the resources, but we need to do it in the classloader
            // of the running application, not the classloader of the test
            // (for a main test, they are not the same)
            //must be done after the TCCL has been set
            Class<?> testResourceManagerClass = startupAction.getClassLoader().loadClass(TestResourceManager.class.getName());
            testResourceManager = TestResourceUtil.TestResourceManagerReflections.createReflectively(testResourceManagerClass,
                    context.getRequiredTestClass(),
                    profile,
                    copyEntriesFromProfile(profileInstance, startupAction.getClassLoader()),
                    profileInstance != null && profileInstance.disableGlobalTestResources(),
                    startupAction.getDevServicesProperties(),
                    Optional.ofNullable(startupAction.getDevServicesNetworkId()));
            TestResourceUtil.TestResourceManagerReflections.initReflectively(testResourceManager, profile);
            Map<String, String> properties = TestResourceUtil.TestResourceManagerReflections
                    .startReflectively(testResourceManager);
            startupAction.overrideConfig(properties);

            testResourceManager.getClass()
                    .getMethod("inject", Object.class)
                    .invoke(testResourceManager, context.getRequiredTestInstance());

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

            uninstallLoggerRedirect();
            QuarkusConsole.uninstallRedirects();
            if (originalCl != null) {
                Thread.currentThread()
                        .setContextClassLoader(originalCl);
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (isIntegrationTest(extensionContext.getRequiredTestClass())) {
            return false;
        }
        Class<?> type = parameterContext.getParameter()
                .getType();
        return type == LaunchResult.class || type == QuarkusMainLauncher.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter()
                .getType();
        Class<? extends QuarkusTestProfile> profile = getQuarkusTestProfile(extensionContext);
        if (type == LaunchResult.class) {
            var launch = extensionContext.getRequiredTestMethod()
                    .getAnnotation(Launch.class);
            if (launch != null) {
                doLaunchAndAssertExitCode(extensionContext, profile, launch);
            } else {
                throw new RuntimeException(
                        "To use 'LaunchResult' as a method parameter, the test must be annotated with '@Launch'");
            }

            return result;
        } else if (type == QuarkusMainLauncher.class) {
            return new QuarkusMainLauncher() {
                @Override
                public LaunchResult launch(String... args) {
                    try {
                        return doLaunch(extensionContext, profile, args);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } else {
            throw new RuntimeException("Parameter type not supported");
        }
    }

    private void doLaunchAndAssertExitCode(ExtensionContext extensionContext, Class<? extends QuarkusTestProfile> profile,
            Launch launch) {
        // HACK: we launch the application in this method instead of in beforeEach in order to ensure that
        // tests that use @BeforeEach will have a chance to run before the application is launched

        String[] arguments = launch.value();
        LaunchResult r;
        try {
            r = doLaunch(extensionContext, profile, arguments);
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
        Class<? extends QuarkusTestProfile> profile = getQuarkusTestProfile(extensionContext);
        if (invocationContext.getArguments()
                .isEmpty()) {
            var launch = extensionContext.getRequiredTestMethod()
                    .getAnnotation(Launch.class);
            if (launch != null) {
                // in this case, resolveParameter has not been called by JUnit, so we need to make sure the application is launched
                doLaunchAndAssertExitCode(extensionContext, profile, launch);
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
