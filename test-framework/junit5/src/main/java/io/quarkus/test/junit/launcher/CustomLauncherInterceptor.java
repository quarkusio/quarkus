package io.quarkus.test.junit.launcher;

import java.io.IOException;

import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherInterceptor;

import io.quarkus.test.junit.classloading.FacadeClassLoader;

public class CustomLauncherInterceptor implements LauncherDiscoveryListener, LauncherInterceptor {

    private static FacadeClassLoader facadeLoader = null;
    // This class might be instantiated several times over the course of an execution, so use a static variable to store a 'true' starting state
    private static ClassLoader origCl = ClassLoader.getSystemClassLoader();

    public CustomLauncherInterceptor() {
    }

    private static boolean isProductionModeTests() {
        return System.getProperty("prod.mode.tests") != null;
    }

    @Override
    public <T> T intercept(Invocation<T> invocation) {
        // Do not do any classloading dance for prod mode tests;
        if (!isProductionModeTests()) {
            initializeFacadeClassLoader();
        }

        T answer = invocation.proceed();

        // Because of how gradle works, in the forked JVM, it loads the test classes *before* JUnit discovery is called.
        // That means that setting the TCCL on discovery is too late, but setting the TCCL on the interception is earlier than we'd like,
        // and causes some problems with config being set on the wrong classloader.

        // It's unclear if it's better to special-case gradle to reduce the impact, or be generic to make things consistent
        if (System.getProperty("org.gradle.test.worker") != null) {
            // This gets called several times; some for creation of LauncherSessionListener instances registered via the ServiceLoader mechanism,
            // some for creation of Launcher instances, and some for calls to Launcher.discover(LauncherDiscoveryRequest), Launcher.execute(TestPlan, TestExecutionListener...), and Launcher.execute(LauncherDiscoveryRequest, TestExecutionListener...)
            // We only know why it was called *after* calling invocation.proceed, sadly
            // The Gradle classloading seems to happen immediately after the ConfigSessionListener is triggered, but before the next launch invocation
            adjustContextClassLoader();
        }

        return answer;

    }

    // Make a facade classloader if needed, so that we can close it at the end of the launcher session
    private void initializeFacadeClassLoader() {
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        // Be aware, this method might be called more than once, for different kinds of invocations; especially for Gradle executions, the executions could happen before the TCCL gets constructed and set by JUnitTestRunner
        // We might not be in the same classloader as the Facade ClassLoader, so use a name comparison instead of an instanceof
        if (currentCl == null
                || (currentCl != facadeLoader && !currentCl.getClass().getName().equals(FacadeClassLoader.class.getName()))) {

            // We don't ever want more than one FacadeClassLoader active, especially since config gets initialised on it.
            // The gradle test execution can make more than one, perhaps because of its threading model.
            if (facadeLoader == null) {
                facadeLoader = new FacadeClassLoader(currentCl);
            }
        }
    }

    @Override
    public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
        // Do not do any classloading dance for prod mode tests;
        // We're too early for config to be available, so just check the system props
        if (!isProductionModeTests()) {
            adjustContextClassLoader();
        }

    }

    private void adjustContextClassLoader() {
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        // Be aware, this method might be called more than once, for different kinds of invocations; especially for Gradle executions, the executions could happen before the TCCL gets constructed and set by JUnitTestRunner
        // We might not be in the same classloader as the Facade ClassLoader, so use a name comparison instead of an instanceof
        if (currentCl == null
                || (currentCl != facadeLoader && !currentCl.getClass().getName().equals(FacadeClassLoader.class.getName()))) {
            origCl = currentCl;
            Thread.currentThread().setContextClassLoader(facadeLoader);
        }
    }

    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
        // Do not close the facade loader at this stage, because discovery finished may be called several times within a single run
        // Ideally we would only clear TCCLs we set, but in practice we want to make sure the TCCL is correct post-discoverym even in continuous testing scenarios where the FCL gets created by the runner
        Thread.currentThread().setContextClassLoader(origCl);
    }

    @Override
    public void close() {

        try {
            // Tidy up classloaders we created, but not ones created upstream
            // Also make sure to reset the TCCL so we don't leave a closed classloader on the thread
            if (facadeLoader != null) {
                facadeLoader.close();
                facadeLoader = null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to close custom classloader", e);
        }
    }
}
