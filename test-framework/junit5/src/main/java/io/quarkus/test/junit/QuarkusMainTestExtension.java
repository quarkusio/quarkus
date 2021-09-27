package io.quarkus.test.junit;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

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
import io.quarkus.deployment.dev.testing.LogCapturingOutputFilter;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;

public class QuarkusMainTestExtension extends AbstractJvmQuarkusTestExtension
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver, BeforeAllCallback, AfterAllCallback {

    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create("io.quarkus.test.main.jvm");

    private static Map<String, String> devServicesProps;

    PrepareResult prepareResult;

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
        var launch = context.getRequiredTestMethod().getAnnotation(Launch.class);
        if (launch != null) {
            String[] arguments = launch.value();
            LaunchResult r = doLaunch(context, arguments);
            Assertions.assertEquals(launch.exitCode(), r.exitCode(),
                    "Exit code did not match, output: " + r.getOutput() + " " + r.getErrorOutput());
            this.result = r;
        }
    }

    private void ensurePrepared(ExtensionContext context) throws Exception {
        Class<? extends QuarkusTestProfile> profile = getQuarkusTestProfile(context);
        if (prepareResult == null) {
            final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();
            PrepareResult result = createAugmentor(context, profile, shutdownTasks);
            prepareResult = result;
        }
    }

    private LaunchResult doLaunch(ExtensionContext context, String[] arguments) throws Exception {
        ensurePrepared(context);
        QuarkusConsole.installRedirects();
        LogCapturingOutputFilter filter = new LogCapturingOutputFilter(prepareResult.curatedApplication, false, false,
                () -> true);
        QuarkusConsole.addOutputFilter(filter);
        try {
            var result = doJavaStart(context, arguments);
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

    private int doJavaStart(ExtensionContext context, String[] arguments) throws Exception {
        TracingHandler.quarkusStarting();
        Closeable testResourceManager = null;
        try {
            StartupAction startupAction = prepareResult.augmentAction.createInitialRuntimeApplication();
            Thread.currentThread().setContextClassLoader(startupAction.getClassLoader());
            return startupAction.runMainClassBlocking(arguments);
        } catch (Throwable e) {

            try {
                if (testResourceManager != null) {
                    testResourceManager.close();
                }
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
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
        if (type == LaunchResult.class) {
            return result;
        } else if (type == QuarkusMainLauncher.class) {
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
        QuarkusConsole.installRedirects();
        currentTestClassStack.push(context.getRequiredTestClass());

    }
}
