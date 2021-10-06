package io.quarkus.it.prodmode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper to collect assertions related to the JDK ForkJoinPool
 * state.
 */
public class ForkJoinPoolAssertions {

    /**
     * We test which Classloader is being used by each thread
     * in the common pool of ForkJoinPool.
     * It is expected that a Quarkus application running in production mode
     * will have set the io.quarkus.bootstrap.runner.RunnerClassLoader as
     * context classloader on each of them, to prevent problems at runtime.
     * 
     * @return true only if all checks are successful.
     */
    static boolean isEachFJThreadUsingQuarkusClassloader() {
        AtomicInteger testedOk = new AtomicInteger();
        final int poolParallelism = ForkJoinPool.getCommonPoolParallelism();
        CountDownLatch allDone = new CountDownLatch(poolParallelism);
        CountDownLatch taskRelease = new CountDownLatch(1);
        if (poolParallelism < 1) {
            System.err
                    .println("Can't test this when ForkJoinPool.getCommonPoolParallelism() has been forced to less than one.");
            return false;
        }
        for (int i = 0; i < poolParallelism; ++i) {
            ForkJoinPool.commonPool().execute(new Runnable() {
                @Override
                public void run() {
                    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                    final String classLoaderImplementationName = contextClassLoader.getClass().getName();
                    if (classLoaderImplementationName.equals(io.quarkus.bootstrap.runner.RunnerClassLoader.class.getName())) {
                        testedOk.incrementAndGet();
                    } else {
                        System.out.println("Unexpected classloader name used in production: " + classLoaderImplementationName);
                    }
                    allDone.countDown();
                    try {
                        taskRelease.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        try {
            if (!allDone.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while trying to scale up the fork join pool");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            taskRelease.countDown();
        }
        return testedOk.get() == poolParallelism;
    }
}
