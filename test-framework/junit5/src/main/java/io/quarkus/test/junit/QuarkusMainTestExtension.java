package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.IntegrationTestUtil.getAdditionalTestResources;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Handler;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.handlers.OutputStreamHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.deployment.dev.testing.LogCapturingOutputFilter;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;

public class QuarkusMainTestExtension extends AbstractJvmQuarkusTestExtension
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver, BeforeAllCallback, AfterAllCallback {

    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create("io.quarkus.test.main.jvm");

    PrepareResult prepareResult;
    private static boolean hasPerTestResources;

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
        var launch = context.getRequiredTestMethod().getAnnotation(Launch.class);
        if (launch != null) {
            String[] arguments = launch.value();
            LaunchResult r = doLaunch(context, profile, arguments);
            Assertions.assertEquals(launch.exitCode(), r.exitCode(),
                    "Exit code did not match, output: " + r.getOutput() + " " + r.getErrorOutput());
            this.result = r;
        }
    }

    private void ensurePrepared(ExtensionContext extensionContext, Class<? extends QuarkusTestProfile> profile)
            throws Exception {
        ExtensionContext.Store store = extensionContext.getStore(ExtensionContext.Namespace.GLOBAL);
        QuarkusTestExtension.ExtensionState state = store.get(QuarkusTestExtension.ExtensionState.class.getName(),
                QuarkusTestExtension.ExtensionState.class);
        boolean wrongProfile = !Objects.equals(profile, quarkusTestProfile);
        // we reload the test resources if we changed test class and if we had or will have per-test test resources
        boolean reloadTestResources = !Objects.equals(extensionContext.getRequiredTestClass(), currentJUnitTestClass)
                && (hasPerTestResources || hasPerTestResources(extensionContext));
        if (wrongProfile || reloadTestResources) {
            if (state != null) {
                try {
                    state.close();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
            prepareResult = null;
        }
        if (prepareResult == null) {
            final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();
            PrepareResult result = createAugmentor(extensionContext, profile, shutdownTasks);
            prepareResult = result;
        }
    }

    private LaunchResult doLaunch(ExtensionContext context, Class<? extends QuarkusTestProfile> selectedProfile,
            String[] arguments) throws Exception {
        ensurePrepared(context, selectedProfile);
        LogCapturingOutputFilter filter = new LogCapturingOutputFilter(prepareResult.curatedApplication, false, false,
                () -> true);
        QuarkusConsole.addOutputFilter(filter);
        try {
            var result = doJavaStart(context, selectedProfile, arguments);
            //merge all the output into one, strip ansi, then split into lines
            List<String> out = Arrays
                    .asList(String.join("", filter.captureOutput()).replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "").split("\n"));
            List<String> err = Arrays
                    .asList(String.join("", filter.captureErrorOutput()).replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "").split("\n"));
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
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        result = null;
    }

    private static Handler ORIGINAL_QUARKUS_CONSOLE_HANDLER = null;
    private static Handler REDIRECT_QUARKUS_CONSOLE_HANDLER = null;

    private static void installLoggerRedirect() throws Exception {
        var rootLogger = LogContext.getLogContext().getLogger("");

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
        var rootLogger = LogContext.getLogContext().getLogger("");
        if (REDIRECT_QUARKUS_CONSOLE_HANDLER != null) {
            rootLogger.addHandler(ORIGINAL_QUARKUS_CONSOLE_HANDLER);
            rootLogger.removeHandler(REDIRECT_QUARKUS_CONSOLE_HANDLER);
        }
    }

    private void flushAllLoggers() {
        Enumeration<String> loggerNames = org.jboss.logmanager.LogContext.getLogContext().getLoggerNames();
        while (loggerNames != null && loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            var logger = org.jboss.logmanager.LogContext.getLogContext().getLogger(loggerName);
            for (Handler h : logger.getHandlers()) {
                h.flush();
            }
        }
    }

    private int doJavaStart(ExtensionContext context, Class<? extends QuarkusTestProfile> profile, String[] arguments)
            throws Exception {
        TracingHandler.quarkusStarting();
        Closeable testResourceManager = null;
        try {
            StartupAction startupAction = prepareResult.augmentAction.createInitialRuntimeApplication();
            Thread.currentThread().setContextClassLoader(startupAction.getClassLoader());
            QuarkusConsole.installRedirects();
            flushAllLoggers();
            installLoggerRedirect();

            QuarkusTestProfile profileInstance = prepareResult.profileInstance;

            //must be done after the TCCL has been set
            testResourceManager = (Closeable) startupAction.getClassLoader().loadClass(TestResourceManager.class.getName())
                    .getConstructor(Class.class, Class.class, List.class, boolean.class, Map.class, Optional.class)
                    .newInstance(context.getRequiredTestClass(),
                            profile != null ? profile : null,
                            getAdditionalTestResources(profileInstance, startupAction.getClassLoader()),
                            profileInstance != null && profileInstance.disableGlobalTestResources(),
                            startupAction.getDevServicesProperties(), Optional.empty());
            testResourceManager.getClass().getMethod("init", String.class).invoke(testResourceManager,
                    profile != null ? profile.getName() : null);
            Map<String, String> properties = (Map<String, String>) testResourceManager.getClass().getMethod("start")
                    .invoke(testResourceManager);
            startupAction.overrideConfig(properties);
            hasPerTestResources = (boolean) testResourceManager.getClass().getMethod("hasPerTestResources")
                    .invoke(testResourceManager);

            testResourceManager.getClass().getMethod("inject", Object.class)
                    .invoke(testResourceManager, context.getRequiredTestInstance());

            var result = startupAction.runMainClassBlocking(arguments);
            flushAllLoggers();
            return result;
        } catch (Throwable e) {
            if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                activateLogging();
            }

            try {
                if (testResourceManager != null) {
                    testResourceManager.close();
                }
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
            uninstallLoggerRedirect();
            QuarkusConsole.uninstallRedirects();
            if (originalCl != null) {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (isIntegrationTest(extensionContext.getRequiredTestClass())) {
            return false;
        }
        Class<?> type = parameterContext.getParameter().getType();
        return type == LaunchResult.class || type == QuarkusMainLauncher.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        Class<? extends QuarkusTestProfile> profile = getQuarkusTestProfile(extensionContext);
        if (type == LaunchResult.class) {
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
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        currentTestClassStack.push(context.getRequiredTestClass());
    }
}
