package io.quarkus.runtime;

import java.io.Closeable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.wildfly.common.Assert;
import org.wildfly.common.lock.Locks;

import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.runtime.shutdown.ShutdownRecorder;

/**
 * The application base class, which is extended and implemented by a generated class which implements the application
 * setup logic. The base class does some basic error checking, and maintains the application state.
 *
 * Note that this class does not manage the application lifecycle in any way, it is solely responsible for starting and
 * stopping the application.
 *
 */
@SuppressWarnings("restriction")
public abstract class Application implements Closeable {

    // WARNING: do not inject a logger here, it's too early: the log manager has not been properly set up yet

    /**
     * The name of the generated application class
     */
    public static final String APP_CLASS_NAME = "io.quarkus.runner.ApplicationImpl";

    private static final int ST_INITIAL = 0;
    private static final int ST_STARTING = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_STOPPING = 3;
    private static final int ST_STOPPED = 4;

    private final Lock stateLock = Locks.reentrantLock();
    private final Condition stateCond = stateLock.newCondition();

    private int state = ST_INITIAL;
    protected static volatile Application currentApplication;

    /**
     * Embedded applications don't set up or modify logging, and don't provide start/
     * stop notifications to the {@link ApplicationStateNotification}.
     */
    private final boolean auxilaryApplication;

    /**
     * Construct a new instance.
     *
     * @param auxilaryApplication
     */
    protected Application(boolean auxilaryApplication) {
        this.auxilaryApplication = auxilaryApplication;
    }

    /**
     * Start the application. If another thread is also trying to start the application, this method waits for that
     * thread to finish starting. Returns immediately if the application is started already. If the application
     * fails to start, an exception is thrown.
     *
     * @param args the command-line arguments
     * @implNote The command line args are not yet used, but at some point we'll want a facility for overriding config and/or
     *           letting the user hook into it.
     */
    public final void start(String[] args) {
        if (!auxilaryApplication) {
            currentApplication = this;
        }
        final Lock stateLock = this.stateLock;
        stateLock.lock();
        try {
            loop: for (;;)
                switch (state) {
                    case ST_INITIAL:
                        break loop; // normal startup
                    case ST_STARTING: {
                        try {
                            stateCond.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw interruptedOnAwaitStart();
                        }
                        break;
                    }
                    case ST_STARTED:
                        return; // all good
                    default: {
                        throw new IllegalStateException("The application is stopping");
                    }
                }
            state = ST_STARTING;
        } finally {
            stateLock.unlock();
        }
        try {
            doStart(args);
        } catch (Throwable t) {
            stateLock.lock();
            final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            try {
                cpr.releaseConfig(cpr.getConfig());
            } catch (IllegalStateException ignored) {
                // just means no config was installed, which is fine
            }
            try {
                state = ST_STOPPED;
                stateCond.signalAll();
            } finally {
                stateLock.unlock();
            }
            if (!auxilaryApplication) {
                ApplicationStateNotification.notifyStartupFailed(t);
            }
            throw t;
        }
        stateLock.lock();
        try {
            state = ST_STARTED;
            stateCond.signalAll();
            if (!auxilaryApplication) {
                ApplicationStateNotification.notifyStartupComplete();
            }
        } finally {
            stateLock.unlock();
        }
    }

    protected abstract void doStart(String[] args);

    public final void close() {
        try {
            stop();
        } finally {
            try {
                ConfigProviderResolver.instance()
                        .releaseConfig(
                                ConfigProviderResolver.instance().getConfig(Thread.currentThread().getContextClassLoader()));
            } catch (Throwable ignored) {

            }
        }
    }

    /**
     * Stop the application. If another thread is also trying to stop the application, this method waits for that
     * thread to finish. Returns immediately if the application is already stopped. If an exception is thrown during
     * stop, that exception is propagated.
     */
    public final void stop() {
        stop(null);
    }

    /**
     * Stop the application. If another thread is also trying to stop the application, this method waits for that
     * thread to finish. Returns immediately if the application is already stopped. If an exception is thrown during
     * stop, that exception is propagated.
     */
    public final void stop(Runnable afterStopTask) {
        Logger logger = Logger.getLogger(Application.class);
        logger.debugf("Stopping application");
        if (logger.isTraceEnabled()) {
            logger.tracef(new RuntimeException("Application Stop Stack Trace"), "Application shutting down");
        }
        final Lock stateLock = this.stateLock;
        stateLock.lock();
        try {
            loop: for (;;)
                switch (state) {
                    case ST_INITIAL:
                        throw new IllegalStateException("The application has not been started");
                    case ST_STARTING: {
                        try {
                            stateCond.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw interruptedOnAwaitStart();
                        }
                        break;
                    }
                    case ST_STARTED:
                        break loop; // normal shutdown
                    case ST_STOPPING: {
                        try {
                            stateCond.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw interruptedOnAwaitStop();
                        }
                        break;
                    }
                    case ST_STOPPED:
                        return; // all good
                    default:
                        throw Assert.impossibleSwitchCase(state);
                }
            state = ST_STOPPING;
        } finally {
            stateLock.unlock();
        }
        Timing.staticInitStopped(auxilaryApplication);
        try {
            ShutdownRecorder.runShutdown();
            doStop();
        } finally {
            if (!auxilaryApplication) {
                currentApplication = null;
            }
            if (afterStopTask != null) {
                try {
                    afterStopTask.run();
                } catch (Throwable t) {
                    Logger.getLogger(Application.class).error("Failed to run stop task", t);
                }
            }
            stateLock.lock();
            try {
                state = ST_STOPPED;
                //note that at the moment if these are started or stopped concurrently
                //the timing will be off
                Timing.printStopTime(getName(), auxilaryApplication);
                stateCond.signalAll();
                if (!auxilaryApplication) {
                    ApplicationStateNotification.notifyApplicationStopped();
                }
            } finally {
                stateLock.unlock();
            }
        }
    }

    public static Application currentApplication() {
        return currentApplication;
    }

    protected abstract void doStop();

    public abstract String getName();

    private static IllegalStateException interruptedOnAwaitStart() {
        return new IllegalStateException("Interrupted while waiting for another thread to start the application");
    }

    private static IllegalStateException interruptedOnAwaitStop() {
        return new IllegalStateException("Interrupted while waiting for another thread to stop the application");
    }

    public void awaitShutdown() {
        final Lock stateLock = this.stateLock;
        stateLock.lock();
        try {
            for (;;) {
                if (state == ST_STOPPED) {
                    return; // all good
                } else {
                    try {
                        stateCond.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw interruptedOnAwaitStop();
                    }
                }
            }
        } finally {
            stateLock.unlock();
        }
    }

    public boolean isStarted() {
        return state == ST_STARTED;
    }
}
