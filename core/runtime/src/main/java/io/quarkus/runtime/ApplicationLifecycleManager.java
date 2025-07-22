package io.quarkus.runtime;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.wildfly.common.lock.Locks;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.runner.RunnerClassLoader;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.graal.DiagnosticPrinter;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.runtime.util.StringUtil;
import io.smallrye.config.ConfigValidationException;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Manages the lifecycle of a Quarkus application.
 *
 * The {@link Application} class is responsible for starting and stopping the application,
 * but nothing else. This class can be used to run both persistent applications that will run
 * till they receive a signal, and command mode applications that will run until the main method
 * returns. This class registers a shutdown hook to properly shut down the application, and handles
 * exiting with the supplied exit code as well as any exception thrown when starting the application.
 *
 * This class should be used to run production and dev mode applications, while test use cases will
 * likely want to just use {@link Application} directly.
 *
 * This class is static, there can only ever be a single application instance running at any time.
 *
 */
public class ApplicationLifecycleManager {

    // used by ShutdownEvent to propagate the information about shutdown reason
    public static volatile ShutdownEvent.ShutdownReason shutdownReason = ShutdownEvent.ShutdownReason.STANDARD;
    private static final BiConsumer<Integer, Throwable> MAIN_EXIT_CODE_HANDLER = new BiConsumer<>() {
        @Override
        public void accept(Integer integer, Throwable cause) {
            Logger logger = Logger.getLogger(Application.class);
            logger.debugf("Shutting down with exit code %s", integer);
            if (logger.isTraceEnabled()) {
                logger.tracef(new RuntimeException("Shutdown Stack Trace"), "Shutdown triggered");
            }
            System.exit(integer);
        }
    };
    private static final Consumer<Boolean> NOOP_ALREADY_STARTED_CALLBACK = new Consumer<>() {
        @Override
        public void accept(Boolean t) {
        }
    };
    private static volatile BiConsumer<Integer, Throwable> defaultExitCodeHandler = MAIN_EXIT_CODE_HANDLER;

    private ApplicationLifecycleManager() {

    }
    // WARNING: do not inject a logger here, it's too early: the log manager has not been properly set up yet

    private static final String DISABLE_SIGNAL_HANDLERS = "DISABLE_SIGNAL_HANDLERS";

    //guard for all state
    private static final Lock stateLock = Locks.reentrantLock();
    private static final Condition stateCond = stateLock.newCondition();
    private static ShutdownHookThread shutdownHookThread;

    private static int exitCode = -1;
    private static volatile boolean shutdownRequested;
    private static volatile Application currentApplication;
    private static boolean vmShuttingDown;
    private static Consumer<Boolean> alreadyStartedCallback = NOOP_ALREADY_STARTED_CALLBACK;

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac");

    public static final String QUARKUS_APPCDS_GENERATE_PROP = "quarkus.appcds.generate";

    public static void run(Application application, String... args) {
        run(application, null, null, args);
    }

    public static void run(Application application, Class<? extends QuarkusApplication> quarkusApplication,
            BiConsumer<Integer, Throwable> exitCodeHandler, String... args) {
        boolean alreadyStarted;
        stateLock.lock();
        try {
            //in tests, we might pass this method an already started application
            //in this case we don't shut it down at the end
            alreadyStarted = application.isStarted();
            alreadyStartedCallback.accept(alreadyStarted);
            if (shutdownHookThread == null) {
                registerHooks(exitCodeHandler == null ? defaultExitCodeHandler : exitCodeHandler);
            }
            if (currentApplication != null && !shutdownRequested) {
                throw new IllegalStateException("Quarkus already running");
            }
            exitCode = -1;
            shutdownRequested = false;
            currentApplication = application;
        } finally {
            stateLock.unlock();
        }
        try {

            application.start(args);
            // now we are started, we either run the main application or just wait to exit
            // when we are in AppCDS generation we can't call the bean container, we just want to fall through to the exit
            if (quarkusApplication != null && !isAppCDSGeneration()) {
                BeanManager beanManager = CDI.current().getBeanManager();
                Set<Bean<?>> beans = beanManager.getBeans(quarkusApplication, Any.Literal.INSTANCE);
                Bean<?> bean = null;
                for (Bean<?> i : beans) {
                    if (i.getBeanClass() == quarkusApplication) {
                        bean = i;
                        break;
                    }
                }
                QuarkusApplication instance;
                if (bean == null) {
                    instance = quarkusApplication.getDeclaredConstructor().newInstance();
                } else {
                    CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
                    instance = (QuarkusApplication) beanManager.getReference(bean, quarkusApplication, ctx);
                }
                int result = -1;
                try {
                    result = instance.run(args);//TODO: argument filtering?
                } finally {
                    stateLock.lock();
                    try {
                        //now we exit
                        if (exitCode == -1 && result != -1) {
                            exitCode = result;
                        }
                        shutdownRequested = true;
                        stateCond.signalAll();
                    } finally {
                        stateLock.unlock();
                    }
                }
            } else {
                longLivedPostBootCleanup();
                stateLock.lock();
                try {
                    while (!shutdownRequested) {
                        Thread.interrupted();
                        stateCond.awaitUninterruptibly();
                    }
                } finally {
                    stateLock.unlock();
                }
            }
        } catch (Exception e) {
            Throwable rootCause = ExceptionUtil.getRootCause(e);
            if (exitCodeHandler == null) {
                Logger applicationLogger = Logger.getLogger(Application.class);
                if (rootCause instanceof QuarkusBindException qbe) {
                    String host = qbe.getHost();
                    int port = qbe.getPort();
                    if (QuarkusBindException.isKnownHost(host)) {
                        applicationLogger.errorf("Port %d seems to be in use by another process. " +
                                "Quarkus may already be running or the port is used by another application.", port);
                        if (IS_WINDOWS) {
                            applicationLogger.warn("Use 'netstat -a -b -n -o' to identify the process occupying the port.");
                            applicationLogger
                                    .warn("You can try to kill it with 'taskkill /PID <pid>' or via the Task Manager.");
                        } else if (IS_MAC) {
                            applicationLogger
                                    .warnf("Use 'netstat -anv | grep %d' to identify the process occupying the port.", port);
                            applicationLogger.warn("You can try to kill it with 'kill -9 <pid>'.");
                        } else {
                            applicationLogger
                                    .warnf("Use 'ss -anop | grep %1$d' or 'netstat -anop | grep %1$d' to identify the process occupying the port.",
                                            port);
                            applicationLogger.warn("You can try to kill it with 'kill -9 <pid>'.");
                        }
                    } else {
                        applicationLogger.errorf("Unable to bind to host: %s and port: %d.", host, port);
                    }

                } else if (rootCause instanceof ConfigurationException || rootCause instanceof ConfigValidationException) {
                    System.err.println(rootCause.getMessage());
                } else if (rootCause instanceof PreventFurtherStepsException
                        && !StringUtil.isNullOrEmpty(rootCause.getMessage())) {
                    System.err.println(rootCause.getMessage());
                } else {
                    applicationLogger.errorv(e, "Failed to start application");
                    ensureConsoleLogsDrained();
                }
            }
            stateLock.lock();
            try {
                shutdownRequested = true;
                stateCond.signalAll();
            } finally {
                stateLock.unlock();
            }
            application.stop();
            int exceptionExitCode = rootCause instanceof PreventFurtherStepsException
                    ? ((PreventFurtherStepsException) rootCause).getExitCode()
                    : 1;
            currentApplication = null;
            (exitCodeHandler == null ? defaultExitCodeHandler : exitCodeHandler).accept(exceptionExitCode, e);
            return;
        } finally {
            try {
                ShutdownHookThread sh = shutdownHookThread;
                shutdownHookThread = null;
                if (sh != null) {
                    Runtime.getRuntime().removeShutdownHook(sh);
                }
            } catch (IllegalStateException ignore) {

            }
        }
        if (!alreadyStarted) {
            application.stop(); //this could have already been called
        }
        currentApplication = null;
        (exitCodeHandler == null ? defaultExitCodeHandler : exitCodeHandler).accept(getExitCode(), null); //this may not be called if shutdown was initiated by a signal
    }

    // this is needed only when async console logging is enabled
    private static void ensureConsoleLogsDrained() {
        AsyncHandler asyncHandler = null;
        for (Handler handler : InitialConfigurator.DELAYED_HANDLER.getHandlers()) {
            if (handler instanceof AsyncHandler) {
                asyncHandler = (AsyncHandler) handler;
                Handler[] nestedHandlers = asyncHandler.getHandlers();
                boolean foundNestedConsoleHandler = false;
                for (Handler nestedHandler : nestedHandlers) {
                    if (nestedHandler instanceof ConsoleHandler) {
                        foundNestedConsoleHandler = true;
                        break;
                    }
                }
                if (!foundNestedConsoleHandler) {
                    asyncHandler = null;
                }
            }
            if (asyncHandler != null) {
                break;
            }
        }
        if (asyncHandler != null) {
            try {
                // all we can do is wait because the thread that takes records off the queue is a daemon thread and there is no way to interact with its lifecycle
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Run some background cleanup once after the application has booted.
     * This will not be invoked for command mode, as it's not worth it for a short-lived process.
     */
    private static void longLivedPostBootCleanup() {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl instanceof RunnerClassLoader) {
            RunnerClassLoader rcl = (RunnerClassLoader) cl;
            rcl.resetInternalCaches();
        }
    }

    private static void registerHooks(final BiConsumer<Integer, Throwable> exitCodeHandler) {
        if (ImageMode.current() == ImageMode.NATIVE_RUN && System.getenv(DISABLE_SIGNAL_HANDLERS) == null) {
            registerSignalHandlers(exitCodeHandler);
        }
        shutdownHookThread = new ShutdownHookThread();
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }

    private static void registerSignalHandlers(final BiConsumer<Integer, Throwable> exitCodeHandler) {
        final SignalHandler diagnosticsHandler = new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                DiagnosticPrinter.printDiagnostics(System.out);
            }
        };
        // the HUP and QUIT signals are not defined for the Windows OpenJDK implementation:
        // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/hotspot/file/7d5c800dae75/src/os/windows/vm/jvm_windows.cpp
        if (IS_WINDOWS) {
            handleSignal("BREAK", diagnosticsHandler);
        } else {
            handleSignal("QUIT", diagnosticsHandler);
        }
    }

    public static Application getCurrentApplication() {
        return currentApplication;
    }

    /**
     *
     * @return The current exit code that would be reported if the application exits
     */
    public static int getExitCode() {
        return exitCode == -1 ? 0 : exitCode;
    }

    /**
     * Exits without supplying an exit code.
     *
     * The application will exit with a code of 0 by default, however if this method is called it is still possible
     * for a different exit code to be set.
     */
    public static void exit() {
        exit(-1);
    }

    public static BiConsumer<Integer, Throwable> getDefaultExitCodeHandler() {
        return defaultExitCodeHandler;
    }

    /**
     *
     * @return <code>true</code> if the VM is shutting down
     */
    public static boolean isVmShuttingDown() {
        return vmShuttingDown;
    }

    /**
     * Sets the default exit code and exception handler for application run through the run method
     * that does not take an exit handler.
     *
     * By default, this will just call {@code System.exit}, however this is not always
     * what is wanted.
     *
     * @param defaultExitCodeHandler the new default exit handler
     */
    public static void setDefaultExitCodeHandler(BiConsumer<Integer, Throwable> defaultExitCodeHandler) {
        if (defaultExitCodeHandler == null) {
            defaultExitCodeHandler = MAIN_EXIT_CODE_HANDLER;
        }
        ApplicationLifecycleManager.defaultExitCodeHandler = defaultExitCodeHandler;
    }

    /**
     * Sets the default exit code handler for application run through the run method
     * that does not take an exit handler.
     *
     * By default, this will just call {@code System.exit}, however this is not always
     * what is wanted.
     *
     * @param defaultExitCodeHandler the new default exit handler
     */
    // Used by StartupActionImpl via reflection
    public static void setDefaultExitCodeHandler(Consumer<Integer> defaultExitCodeHandler) {
        BiConsumer<Integer, Throwable> biConsumer = defaultExitCodeHandler == null ? null
                : (exitCode, cause) -> defaultExitCodeHandler.accept(exitCode);
        setDefaultExitCodeHandler(biConsumer);
    }

    @SuppressWarnings("unused")
    // Used by StartupActionImpl via reflection
    public static void setAlreadyStartedCallback(Consumer<Boolean> alreadyStartedCallback) {
        ApplicationLifecycleManager.alreadyStartedCallback = alreadyStartedCallback != null ? alreadyStartedCallback
                : NOOP_ALREADY_STARTED_CALLBACK;
    }

    /**
     * Signals that the application should exit with the given code.
     *
     * Note that the first positive exit code will 'win', so if the exit code
     * has already been set then the exit code will be ignored.
     *
     * @param code The exit code
     */
    public static void exit(int code) {
        stateLock.lock();
        try {
            if (code >= 0 && exitCode == -1) {
                exitCode = code;
            }
            if (shutdownRequested) {
                return;
            }
            shutdownRequested = true;
            stateCond.signalAll();
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Waits for the shutdown process to be initiated.
     */
    public static void waitForExit() {
        stateLock.lock();
        try {
            while (!shutdownRequested) {
                stateCond.awaitUninterruptibly();
            }
        } finally {
            stateLock.unlock();
        }
    }

    static class ShutdownHookThread extends Thread {

        ShutdownHookThread() {
            super("Shutdown thread");
            setDaemon(false);
        }

        @Override
        public void run() {
            stateLock.lock();
            vmShuttingDown = true;
            //we just request shutdown and unblock the main thread
            //we let the application main thread take care of actually exiting
            //TODO: if the main thread is not actively waiting to exit should we interrupt it?
            shutdownRequested = true;
            // so long as this thread is invoked, the app shutdown is considered non-standard
            shutdownReason = ShutdownEvent.ShutdownReason.NON_STANDARD;
            try {
                stateCond.signalAll();
            } finally {
                stateLock.unlock();
            }
            //take a reliable reference before changing the application state:
            final Application app = currentApplication;
            if (app != null) {
                if (app.isStarted()) {
                    // On CLI apps, SIGINT won't call io.quarkus.runtime.Application#stop(),
                    // making the awaitShutdown() below block the application termination process
                    // It should be a noop if called twice anyway
                    app.stop();
                }
                app.awaitShutdown();
            }
            currentApplication = null;
            System.out.flush();
            System.err.flush();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private static void handleSignal(final String signal, final SignalHandler handler) {
        try {
            Signal.handle(new Signal(signal), handler);
        } catch (IllegalArgumentException ignored) {
            // Do nothing
        }
    }

    public static boolean isAppCDSGeneration() {
        return Boolean.parseBoolean(System.getProperty(ApplicationLifecycleManager.QUARKUS_APPCDS_GENERATE_PROP, "false"));
    }
}
