package io.quarkus.bootstrap.forkjoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * This implementation of JDK's ForkJoinPool.ForkJoinWorkerThreadFactory
 * needs to be enabled by setting system property {@code java.util.concurrent.ForkJoinPool.common.threadFactory}
 * to this class name, so to allow Quarkus to set the contextual classloader to the correct
 * runtime classloader for each thread being started by the common pool.
 * Otherwise the system classloader will be set, which is unable to load all application resources.
 */
public final class QuarkusForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    public QuarkusForkJoinWorkerThreadFactory() {
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new QuarkusForkJoinWorkerThread(pool);
    }

}
