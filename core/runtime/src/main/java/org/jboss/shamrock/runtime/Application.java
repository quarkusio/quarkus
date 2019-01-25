/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.runtime;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.graal.DiagnosticPrinter;
import org.jboss.threads.Locks;
import org.wildfly.common.Assert;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * The application base class, which is extended and implemented by a generated class which implements the application
 * setup logic.  The base class does some basic error checking.
 */
@SuppressWarnings("restriction")
public abstract class Application {
    private static final int ST_INITIAL = 0;
    private static final int ST_STARTING = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_STOPPING = 3;
    private static final int ST_STOPPED = 4;
    private static final int ST_EXIT = 5;

    private final Lock stateLock = Locks.reentrantLock();
    private final Condition stateCond = stateLock.newCondition();

    private int state = ST_INITIAL;
    private volatile boolean shutdownRequested;

    /**
     * Construct a new instance.
     */
    protected Application() {
    }

    /**
     * Start the application.  If another thread is also trying to start the application, this method waits for that
     * thread to finish starting.  Returns immediately if the application is started already.  If the application
     * fails to start, an exception is thrown.
     *
     * @param args the command-line arguments
     * @implNote The command line args are not yet used, but at some point we'll want a facility for overriding config and/or
     * letting the user hook into it.
     */
    public final void start(@SuppressWarnings("unused") String[] args) {
        final Lock stateLock = this.stateLock;
        stateLock.lock();
        try {
            loop: for (;;) switch (state) {
                case ST_INITIAL: break loop; // normal startup
                case ST_STARTING: {
                    try {
                        stateCond.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw interruptedOnAwaitStart();
                    }
                    break;
                }
                case ST_STARTED: return; // all good
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
            try {
                state = ST_STOPPED;
                stateCond.signalAll();
            } finally {
                stateLock.unlock();
            }
            throw t;
        }
        stateLock.lock();
        try {
            state = ST_STARTED;
            stateCond.signalAll();
        } finally {
            stateLock.unlock();
        }
    }

    protected abstract void doStart(String[] args);

    /**
     * Stop the application.  If another thread is also trying to stop the application, this method waits for that
     * thread to finish.  Returns immediately if the application is already stopped.  If an exception is thrown during
     * stop, that exception is propagated.
     */
    public final void stop() {
        final Lock stateLock = this.stateLock;
        stateLock.lock();
        try {
            loop: for (;;) switch (state) {
                case ST_INITIAL: throw new IllegalStateException("The application has not been started");
                case ST_STARTING: {
                    try {
                        stateCond.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw interruptedOnAwaitStart();
                    }
                    break;
                }
                case ST_STARTED: break loop; // normal shutdown
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
                case ST_EXIT: return; // all good
                default: throw Assert.impossibleSwitchCase(state);
            }
            state = ST_STOPPING;
        } finally {
            stateLock.unlock();
        }
        Timing.staticInitStopped();
        try {
            doStop();
        } finally {
            stateLock.lock();
            try {
                state = ST_STOPPED;
                Timing.printStopTime();
                stateCond.signalAll();
            } finally {
                stateLock.unlock();
            }
        }
    }

    protected abstract void doStop();

    /**
     * Run the application as if it were in a standalone JVM.
     */
    public final void run(String[] args) {
        try {
            if (ImageInfo.inImageRuntimeCode()) {
                final SignalHandler handler = new SignalHandler() {
                    @Override
                    public void handle(final Signal signal) {
                        System.exit(0);
                    }
                };
                Signal.handle(new Signal("INT"), handler);
                Signal.handle(new Signal("TERM"), handler);
                Signal.handle(new Signal("QUIT"), new SignalHandler() {
                    @Override
                    public void handle(final Signal signal) {
                        DiagnosticPrinter.printDiagnostics(System.out);
                    }
                });
            }
            final ShutdownHookThread shutdownHookThread = new ShutdownHookThread(Thread.currentThread());
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
            start(args);
            try {
                while (! shutdownRequested) {
                    Thread.interrupted();
                    LockSupport.park(shutdownHookThread);
                }
            } finally {
                stop();
            }
        } finally {
            exit();
        }
    }

    private void exit() {
        stateLock.lock();
        try {
            System.out.flush();
            System.err.flush();
            state = ST_EXIT;
            stateCond.signalAll();
            // code beyond this point may not run
        } finally {
            stateLock.unlock();
        }
    }

    private static IllegalStateException interruptedOnAwaitStart() {
        return new IllegalStateException("Interrupted while waiting for another thread to start the application");
    }

    private static IllegalStateException interruptedOnAwaitStop() {
        return new IllegalStateException("Interrupted while waiting for another thread to stop the application");
    }

    class ShutdownHookThread extends Thread {
        private final Thread mainThread;

        ShutdownHookThread(Thread mainThread) {
            super("Shutdown thread");
            this.mainThread = mainThread;
            setDaemon(false);
        }

        @Override
        public void run() {
            shutdownRequested = true;
            LockSupport.unpark(mainThread);
            final Lock stateLock = Application.this.stateLock;
            final Condition stateCond = Application.this.stateCond;
            stateLock.lock();
            try {
                while (state != ST_EXIT) {
                    stateCond.awaitUninterruptibly();
                }
            } finally {
                stateLock.unlock();
            }
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
