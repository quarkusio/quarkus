package io.quarkus.test.junit.launcher;

import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import io.quarkus.test.junit.classloading.FacadeClassLoader;

public class CustomLauncherInterceptor implements LauncherDiscoveryListener, LauncherSessionListener {

    private static FacadeClassLoader facadeLoader = null;
    // Also use a static variable to store a 'first' starting state that we can reset to
    private static ClassLoader origCl = null;
    private static boolean discoveryStarted = false;

    public CustomLauncherInterceptor() {
    }

    private static boolean isProductionModeTests() {
        // We're too early for config to be available, so just check the system props
        return System.getProperty("prod.mode.tests") != null;
    }

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        /*
         * For gradle, test class loading happens fairly shortly after this is called,
         * before the formal discovery phase. So we need to intercept the TCCL.
         *
         * However, the Eclipse runner calls this twice, and the second invocation happens after discovery,
         * which means there is no one to unset the TCCL. That breaks integration tests, so we
         * need to add an ugly guard to not adjust the TCCL the second time round in that scenario.
         * We do not do any classloading dance for prod mode tests.
         */
        boolean isEclipse = System.getProperty("sun.java.command") != null
                && System.getProperty("sun.java.command").contains("JUnit5TestLoader");
        boolean shouldSkipSettingTCCL = isEclipse && discoveryStarted;

        if (!isProductionModeTests() && !shouldSkipSettingTCCL) {
            actuallyIntercept();
        }

    }

    private void actuallyIntercept() {
        if (origCl == null) {
            origCl = Thread.currentThread()
                    .getContextClassLoader();
        }
        // We might not be in the same classloader as the Facade ClassLoader, so use a name comparison instead of an instanceof
        initializeFacadeClassLoader();
        adjustContextClassLoader();

        // It's tempting to tidy up in a finally block by resetting the TCCL, but the gradle tests
        // do discovery 'between' invocation blocks, and outside the main

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
        discoveryStarted = true;
        // If anything comes through this method for which there are non-null classloaders on the selectors, that will bypass our classloading
        // To check that case, the code would be something like this. We could detect and warn early, and possibly even filter that test out, but that's not necessarily a better UX than failing later
        // request.getSelectorsByType(ClassSelector.class).stream().map(ClassSelector::getClassLoader) ... and then check for non-emptiness on that field

        // Do not do any classloading dance for prod mode tests;
        if (!isProductionModeTests()) {
            initializeFacadeClassLoader();
            adjustContextClassLoader();
        }

    }

    private void adjustContextClassLoader() {
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        // Be aware, this method might be called more than once, for different kinds of invocations; especially for Gradle executions, the executions could happen before the TCCL gets constructed and set by JUnitTestRunner
        // We might not be in the same classloader as the Facade ClassLoader, so use a name comparison instead of an instanceof
        if (currentCl == null
                || (currentCl != facadeLoader && !currentCl.getClass().getName().equals(FacadeClassLoader.class.getName()))) {
            Thread.currentThread().setContextClassLoader(facadeLoader);
        }
    }

    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {

        if (!isProductionModeTests()) {
            // We need to support two somewhat incompatible scenarios.
            // If there are user extensions present which implement `ExecutionCondition`, and they call config in `evaluateExecutionCondition`,
            // they need the TCCL to be right for reading config (that is, the app classloader)
            // On the other hand, if the QuarkusTestExtension is registered by a service loader mechanism, it gets loaded after the discovery phase finishes,
            // so needs the TCCL to still be the facade classloader.
            // This compromise does mean you can't use the service loader mechanism to avoid having to use `@QuarkusTest` and also use Quarkus config in your own test extensions, but that combination is very unlikely.
            if (!facadeLoader.isServiceLoaderMechanism()) {
                // Do not close the facade loader at this stage, because discovery finished may be called several times within a single run
                // Ideally we would reset to what the TCCL was when we started discovery, but we can't,
                // because the intercept method will have set something before the discovery start is triggered.
                // So, rather annoyingly and clumsily, reset the TCCL to what it was when the first interception happened
                Thread.currentThread().setContextClassLoader(origCl);

            }
        }
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {

        try {
            // Tidy up classloaders we created, but not ones created upstream
            // Also make sure to reset the TCCL so we don't leave a closed classloader on the thread
            if (facadeLoader != null) {

                // Reset the TCCL if it's one we set, but not otherwise
                if (Thread.currentThread().getContextClassLoader() == facadeLoader) {
                    Thread.currentThread().setContextClassLoader(origCl);
                }

                facadeLoader.close();
                facadeLoader = null;

            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to close custom classloader", e);
        }
    }
}
