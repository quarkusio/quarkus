package io.quarkus.runtime;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.wildfly.common.lock.Locks;

import com.oracle.svm.core.OS;

import io.quarkus.runtime.graal.DiagnosticPrinter;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Manages the lifecycle of a Quarkus application.
 *
 * The {@link Application} class is responsible for starting and stopping the application,
 * but nothing else. This class can be used to run both persistent applications that will run
 * till they receive a signal, and command mode applications that will run until the main method
 * returns. This class registers a shutdown hook to properly shut down the application, and handles
 * exiting with the supplied exit code.
 *
 * This class should be used to run production and dev mode applications, while test use cases will
 * likely want to just use {@link Application} directly.
 *
 * This class is static, there can only ever be a single application instance running at any time.
 *
 */
public class ApplicationLifecycleManager {

    private static volatile Consumer<Integer> defaultExitCodeHandler = new Consumer<Integer>() {
        @Override
        public void accept(Integer integer) {
            System.exit(integer);
        }
    };

    private ApplicationLifecycleManager() {

    }
    // WARNING: do not inject a logger here, it's too early: the log manager has not been properly set up yet

    private static final String DISABLE_SIGNAL_HANDLERS = "DISABLE_SIGNAL_HANDLERS";

    //guard for all state
    private static final Lock stateLock = Locks.reentrantLock();
    private static final Condition stateCond = stateLock.newCondition();

    private static int exitCode = -1;
    private static boolean shutdownRequested;
    private static Application currentApplication;
    private static boolean hooksRegistered;
    private static boolean vmShuttingDown;

    public static final void run(Application application, String... args) {
        run(application, null, null, args);
    }

    public static final void run(Application application, Class<? extends QuarkusApplication> quarkusApplication,
            Consumer<Integer> exitCodeHandler, String... args) {
        stateLock.lock();
        //in tests we might pass this method an already started application
        //in this case we don't shut it down at the end
        boolean alreadyStarted = application.isStarted();
        if (!hooksRegistered) {
            registerHooks();
            hooksRegistered = true;
        }
        if (currentApplication != null && !shutdownRequested) {
            throw new IllegalStateException("Quarkus already running");
        }
        try {
            exitCode = -1;
            shutdownRequested = false;
            currentApplication = application;
        } finally {
            stateLock.unlock();
        }
        try {
            application.start(args);
            //now we are started, we either run the main application or just wait to exit
            if (quarkusApplication != null) {
                Set<Bean<?>> beans = CDI.current().getBeanManager().getBeans(quarkusApplication, new Any.Literal());
                Bean<?> bean = null;
                for (Bean<?> i : beans) {
                    if (i.getBeanClass() == quarkusApplication) {
                        bean = i;
                        break;
                    }
                }
                QuarkusApplication instance;
                if (bean == null) {
                    instance = quarkusApplication.newInstance();
                } else {
                    CreationalContext<?> ctx = CDI.current().getBeanManager().createCreationalContext(bean);
                    instance = (QuarkusApplication) CDI.current().getBeanManager().getReference(bean,
                            quarkusApplication, ctx);
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
                stateLock.lock();
                try {
                    while (!shutdownRequested) {
                        Thread.interrupted();
                        stateCond.await();
                    }
                } finally {
                    stateLock.unlock();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(Application.class).error("Error running Quarkus application", e);
            stateLock.lock();
            try {
                shutdownRequested = true;
                stateCond.signalAll();
            } finally {
                stateLock.unlock();
            }
            application.stop(new Runnable() {
                @Override
                public void run() {
                    (exitCodeHandler == null ? defaultExitCodeHandler : exitCodeHandler).accept(1);
                }
            });
            return;
        }
        if (!alreadyStarted) {
            application.stop(); //this could have already been called
        }
        (exitCodeHandler == null ? defaultExitCodeHandler : exitCodeHandler).accept(getExitCode()); //this may not be called if shutdown was initiated by a signal
    }

    private static void registerHooks() {
        if (ImageInfo.inImageRuntimeCode() && System.getenv(DISABLE_SIGNAL_HANDLERS) == null) {
            registerSignalHandlers();
        }
        final ShutdownHookThread shutdownHookThread = new ShutdownHookThread();
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }

    private static void registerSignalHandlers() {
        final SignalHandler handler = new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                System.exit(signal.getNumber() + 0x80);
            }
        };
        final SignalHandler quitHandler = new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                DiagnosticPrinter.printDiagnostics(System.out);
            }
        };
        handleSignal("INT", handler);
        handleSignal("TERM", handler);
        // the HUP and QUIT signals are not defined for the Windows OpenJDK implementation:
        // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/hotspot/file/7d5c800dae75/src/os/windows/vm/jvm_windows.cpp
        if (OS.getCurrent() == OS.WINDOWS) {
            handleSignal("BREAK", quitHandler);
        } else {
            handleSignal("HUP", handler);
            handleSignal("QUIT", quitHandler);
        }
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

    public static Consumer<Integer> getDefaultExitCodeHandler() {
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
     * Sets the default exit code handler for application run through the run method
     * that does not take an exit handler.
     * 
     * By default this will just call System.exit, however this is not always
     * what is wanted.
     * 
     * @param defaultExitCodeHandler
     */
    public static void setDefaultExitCodeHandler(Consumer<Integer> defaultExitCodeHandler) {
        Objects.requireNonNull(defaultExitCodeHandler);
        ApplicationLifecycleManager.defaultExitCodeHandler = defaultExitCodeHandler;
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
            try {
                stateCond.signalAll();
            } finally {
                stateLock.unlock();
            }
            currentApplication.awaitShutdown();
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
}
