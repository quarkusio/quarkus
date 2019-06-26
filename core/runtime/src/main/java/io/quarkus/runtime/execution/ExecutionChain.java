package io.quarkus.runtime.execution;

import java.io.Flushable;
import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

import org.wildfly.common.Assert;

import com.oracle.svm.core.annotate.AlwaysInline;

import io.quarkus.runtime.Timing;

/**
 * Application startup chain node.
 */
public final class ExecutionChain implements ExecutionHandler, Serializable {
    private static final long serialVersionUID = 7360468681846082188L;

    public static final String QUARKUS_FEATURES = "quarkus.features";
    public static final String QUARKUS_VERSION = "quarkus.version";

    private final ExecutionChain next;
    private final ExecutionHandler handler;

    /**
     * Construct a new instance. This should generally be done by the container during static initialization, not by
     * the user.
     *
     * @param next the next item in the chain, or {@code null} to indicate the end of the chain
     * @param handler the corresponding startup handler (must not be {@code null})
     */
    public ExecutionChain(final ExecutionChain next, final ExecutionHandler handler) {
        Assert.checkNotNullParam("handler", handler);
        this.next = next;
        this.handler = handler;
    }

    /**
     * Pass control to the next startup handler. This method may block indefinitely, and will only return upon ordered
     * application shutdown.
     *
     * @param context the startup context to pass to the next step (must not be {@code null})
     * @return the exit code
     * @throws Exception if the next startup handler fails
     */
    @AlwaysInline("Application startup")
    public int proceed(ExecutionContext context) throws Exception {
        Assert.checkNotNullParam("context", context);
        if (next != null) {
            return next.run(context);
        } else {
            final Object features = context.as(MapValuesExecutionContext.class).getValue(QUARKUS_FEATURES);
            final Object version = context.as(MapValuesExecutionContext.class).getValue(QUARKUS_VERSION);
            Timing.printStartupTime(String.valueOf(version), String.valueOf(features));
            return Execution.signalUpAndAwaitStop();
        }
    }

    @AlwaysInline("Application startup")
    int run(ExecutionContext context) throws Exception {
        int res;
        res = Execution.starting();
        if (res >= 0) {
            // exit early
            return res;
        }
        // IntelliJ is a bit confused by this
        //noinspection TryWithIdenticalCatches
        try {
            res = handler.run(this, context);
        } catch (Exception t) {
            throw Execution.stoppingException(t);
        } catch (Error t) {
            throw Execution.stoppingException(t);
        } catch (Throwable t) {
            throw Execution.stoppingException(new UndeclaredThrowableException(t));
        }
        Execution.stopping(res);
        return res;
    }

    /**
     * Start the application represented by this execution chain in a background thread.
     *
     * @param initialContext the initial execution context
     * @throws ExecutionException if start failed
     * @throws AsynchronousExitException if an asynchronous exit has occurred
     */
    public void startAsynchronously(ExecutionContext initialContext) throws ExecutionException, AsynchronousExitException {
        Assert.checkNotNullParam("initialContext", initialContext);
        final Runnable task = new Runnable() {
            public void run() {
                try {
                    ExecutionChain.this.run(initialContext);
                } catch (Throwable t) {
                    Execution.thrown = t;
                    Execution.lastStop();
                    Timing.printStopTime();
                    return;
                }
                Execution.lastStop();
                Timing.printStopTime();
            }
        };
        Execution.firstStart(true);
        Thread thread = new Thread(task, "Quarkus Thread");
        try {
            thread.start();
        } catch (Throwable t) {
            Execution.lastStop();
            Timing.printStopTime();
            throw t;
        }
        Execution.awaitServerUp();
    }

    /**
     * Start the application represented by this execution chain, exiting the JVM when the chain returns. Thrown exceptions will
     * be logged. This method is called directly from the initial {@code main} entry point. This method never returns.
     *
     * @param initialContext the initial execution context (must not be {@code null})
     */
    @AlwaysInline("Application startup")
    public void startAndExit(ExecutionContext initialContext) {
        Assert.checkNotNullParam("initialContext", initialContext);
        try {
            Execution.firstStart(false);
        } catch (AsynchronousExitException e) {
            // the exit code is not used if this is a real async exit
            safeFlush(System.out);
            safeFlush(System.err);
            System.exit(Execution.EXIT_EXCEPTION);
        }
        final int res;
        try {
            res = run(initialContext);
        } catch (AsynchronousExitException ignored) {
            // the exit code is not used if this is a real async exit
            safeFlush(System.out);
            safeFlush(System.err);
            Execution.lastStop();
            System.exit(Execution.EXIT_EXCEPTION);
            throw Assert.unreachableCode();
        } catch (Throwable t) {
            try {
                System.err.print("An exception was thrown: ");
                t.printStackTrace(System.err);
            } catch (Throwable ignored) {
            }
            safeFlush(System.out);
            safeFlush(System.err);
            Execution.lastStop();
            System.exit(Execution.EXIT_EXCEPTION);
            throw Assert.unreachableCode();
        }
        safeFlush(System.out);
        safeFlush(System.err);
        Timing.printStopTime();
        System.exit(res);
        throw Assert.unreachableCode();
    }

    private static void safeFlush(Flushable flushable) {
        try {
            flushable.flush();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Determine whether this chain has a succeeding element.
     *
     * @return {@code true} if there is a successor, {@code false} otherwise
     */
    @AlwaysInline("Application startup")
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Act as an execution handler but run this execution chain in place of the original one. The original execution
     * chain is not run.
     *
     * @param chain the original execution chain (ignored)
     * @param context the application execution context (not {@code null})
     * @return the exit code from this chain
     * @throws Exception if a handler throws an uncaught exception
     */
    @AlwaysInline("Application startup")
    public int run(final ExecutionChain chain, final ExecutionContext context) throws Exception {
        return run(context);
    }
}
