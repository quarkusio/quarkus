package io.quarkus.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.BiConsumer;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.jboss.logging.Logger;

import io.quarkus.launcher.QuarkusLauncher;
import io.quarkus.runtime.logging.JBossVersion;

/**
 * The entry point for applications that use a main method. Quarkus will shut down when the main method returns.
 *
 * If this application has already been generated then it will be run directly, otherwise it will be launched
 * in dev mode and augmentation will be done automatically.
 *
 * If an application does not want to immediately shut down then {@link #waitForExit()} should be called, which
 * will block until shutdown is initiated, either from an external signal or by a call to one of the exit methods.
 *
 * If no main class is specified then one is generated automatically that will simply wait to exit after Quarkus is booted.
 *
 */
public class Quarkus {

    //WARNING: this is too early to inject a logger
    //private static final Logger log = Logger.getLogger(Quarkus.class);

    private static Closeable LAUNCHED_FROM_IDE;

    /**
     * Runs a quarkus application, that will run until the provided {@link QuarkusApplication} has completed.
     *
     * Note that if this is run from the IDE the application will run in a different class loader to the
     * calling class. It is recommended that the calling class do no logic, and instead this logic should
     * go into the QuarkusApplication.
     *
     * @param quarkusApplication The application to run, or null
     * @param args The command line parameters
     */
    public static void run(Class<? extends QuarkusApplication> quarkusApplication, String... args) {
        run(quarkusApplication, null, args);
    }

    /**
     * Runs a quarkus application, that will run until the provided {@link QuarkusApplication} has completed.
     *
     * Note that if this is run from the IDE the application will run in a different class loader to the
     * calling class. It is recommended that the calling class do no logic, and instead this logic should
     * go into the QuarkusApplication.
     *
     * @param quarkusApplication The application to run, or null
     * @param exitHandler The handler that is called with the exit code and any exception (if any) thrown when the application
     *        has finished
     * @param args The command line parameters
     */
    public static void run(Class<? extends QuarkusApplication> quarkusApplication, BiConsumer<Integer, Throwable> exitHandler,
            String... args) {
        try {
            JBossVersion.disableVersionLogging();
            System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
            System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory",
                    "io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory");
            //production and common dev mode path
            //we already have an application, run it directly
            Class<? extends Application> appClass = (Class<? extends Application>) Class.forName(Application.APP_CLASS_NAME,
                    false, Thread.currentThread().getContextClassLoader());
            Application application = appClass.getDeclaredConstructor().newInstance();
            ApplicationLifecycleManager.run(application, quarkusApplication, exitHandler, args);
            return;
        } catch (ClassNotFoundException e) {
            //ignore, this happens when running in dev mode
        } catch (Exception e) {
            if (exitHandler != null) {
                exitHandler.accept(1, e);
            } else {
                Logger.getLogger(Quarkus.class).error("Error running Quarkus", e);
                ApplicationLifecycleManager.getDefaultExitCodeHandler().accept(1, e);
            }
            return;
        }

        //dev mode path, i.e. launching from the IDE
        //this is not the quarkus:dev path as it will augment before
        //calling this method
        launchFromIDE(quarkusApplication, args);

    }

    private static void launchFromIDE(Class<? extends QuarkusApplication> quarkusApplication, String... args) {
        //some trickery, get the class that has invoked us, and use this to figure out the
        //classes root
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int pos = 2;
        while (stackTrace[pos].getClassName().equals(Quarkus.class.getName())) {
            pos++;
        }
        String callingClass = stackTrace[pos].getClassName();
        LAUNCHED_FROM_IDE = QuarkusLauncher.launch(callingClass,
                quarkusApplication == null ? null : quarkusApplication.getName(), args);
    }

    private static void terminateForIDE() {
        if (LAUNCHED_FROM_IDE != null) {
            try {
                LAUNCHED_FROM_IDE.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts a quarkus application, that will run until it either receives a signal (e.g. user presses ctrl+c)
     * or one of the exit methods is called.
     *
     * This method does not return, as System.exit() is called after the application is finished.
     *
     * @param args The command line parameters
     */
    public static void run(String... args) {
        run(null, args);
    }

    /**
     * Exits the application in an async manner. Calling this method
     * will initiate the Quarkus shutdown process, and then immediately return.
     *
     * This method will unblock the {@link #waitForExit()} method.
     *
     * Note that if the main thread is executing a Quarkus application this will only take
     * effect if {@link #waitForExit()} has been called, otherwise the application will continue
     * to execute (i.e. this does not initiate the shutdown process, it just signals the main
     * thread that the application is done so that shutdown can run when the main thread returns).
     *
     * The error code supplied here will override the value returned from the main application.
     *
     * @param code The exit code. This may be overridden if an exception occurs on shutdown
     */
    public static void asyncExit(int code) {
        terminateForIDE();
        ApplicationLifecycleManager.exit(code);
    }

    /**
     * Exits the application in an async manner. Calling this method
     * will initiate the Quarkus shutdown process, and then immediately return.
     *
     * This method will unblock the {@link #waitForExit()} method.
     *
     * Note that if the main thread is executing a Quarkus application this will only take
     * effect if {@link #waitForExit()} has been called, otherwise the application will continue
     * to execute (i.e. this does not initiate the shutdown process, it just signals the main
     * thread that the application is done so that shutdown can run when the main thread returns).
     *
     */
    public static void asyncExit() {
        terminateForIDE();
        ApplicationLifecycleManager.exit(-1);
    }

    /**
     * Method that will block until the Quarkus shutdown process is initiated.
     *
     * Note that this unblocks as soon as the shutdown process starts, not after it
     * has finished.
     *
     * {@link QuarkusApplication} implementations that wish to run some logic on startup, and
     * then run should call this method.
     */
    public static void waitForExit() {
        ApplicationLifecycleManager.waitForExit();

    }

    /**
     * Starts the shutdown process, then waits for the application to shut down.
     *
     * Must not be called by the main thread, or a deadlock will result.
     */
    public static void blockingExit() {
        Application app = Application.currentApplication();
        asyncExit();
        if (app != null) {
            app.awaitShutdown();
        }
    }

    private static class QuarkusCracBootstrapResource implements Resource {
        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {

        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
            manualStart();
            Logger.getLogger(Quarkus.class).info("Started Quarkus via CRAC afterRestore()");
        }
    }

    private static Application manualApp;
    private static final int MANUAL_BEGIN = 0;
    private static final int MANUAL_BEGIN_INITIALIZATION = 1;
    private static final int MANUAL_INITALIZED = 2;
    private static final int MANUAL_STARTING = 3;
    private static final int MANUAL_STARTED = 4;
    private static final int MANUAL_FAILURE = 5;
    private static volatile int manualState = MANUAL_BEGIN;
    private static final Object manualLock = new Object();

    /**
     * Manual initialization of Quarkus runtime in cases where
     * Quarkus does not have control over main() i.e. Lambda or Azure Functions.
     *
     * This method will trigger static initialization for
     * Quarkus and register a handler for CRAC.
     *
     */
    public static void manualInitialize() {
        int tmpState = manualState;
        if (tmpState == MANUAL_FAILURE)
            throw new RuntimeException("Quarkus manual bootstrap failed");
        if (tmpState > MANUAL_BEGIN)
            return;
        synchronized (manualLock) {
            tmpState = manualState;
            if (tmpState > MANUAL_BEGIN)
                return;
            if (tmpState == MANUAL_FAILURE)
                throw new RuntimeException("Quarkus manual bootstrap failed");
            manualState = tmpState = MANUAL_BEGIN_INITIALIZATION;
        }

        try {
            // if Application instantiation is removed from manualInit()
            // then Class.forName() should for class static initialization of ApplicationImpl
            Class appClass = Class.forName("io.quarkus.runner.ApplicationImpl");
            manualApp = (Application) appClass.getDeclaredConstructor().newInstance();
            manualState = MANUAL_INITALIZED;
            Core.getGlobalContext().register(new QuarkusCracBootstrapResource());
        } catch (Exception e) {
            manualState = MANUAL_FAILURE;
            throw new RuntimeException("Quarkus manual initialization failed", e);
        }
    }

    /**
     * Manual startup of Quarkus runtime in cases where
     * Quarkus does not have control over main() i.e. Lambda or Azure Functions.
     *
     * This method will call Application.start() and register shutdown hook
     * It will fail if Quarkus.manualInitialize() has not been called.
     *
     *
     */
    public static void manualStart() {
        int tmpState = manualState;
        if (tmpState == MANUAL_FAILURE)
            throw new IllegalStateException("Quarkus failed to start up");
        if (tmpState >= MANUAL_STARTING)
            return;
        synchronized (manualLock) {
            tmpState = manualState;
            if (tmpState == MANUAL_FAILURE)
                throw new RuntimeException("Quarkus manual bootstrap failed");
            if (tmpState >= MANUAL_STARTING)
                return;
            if (tmpState != MANUAL_INITALIZED)
                throw new IllegalStateException("Quarkus manual start cannot proceed as manual initialization did not run");
            manualState = tmpState = MANUAL_STARTING;
        }
        try {
            String[] args = {};
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    manualApp.stop();
                }
            });
            manualApp.start(args);
        } catch (RuntimeException e) {
            manualState = MANUAL_FAILURE;
            throw e;
        }
        manualState = MANUAL_STARTED;
    }

}
