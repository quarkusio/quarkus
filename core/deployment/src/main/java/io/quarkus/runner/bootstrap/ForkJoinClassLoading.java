package io.quarkus.runner.bootstrap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

public class ForkJoinClassLoading {

    private static final Logger log = Logger.getLogger(ForkJoinClassLoading.class.getName());

    /**
     * A yucky hack, basically attempt to make sure every thread in the common pool has
     * the correct CL.
     * <p>
     * It's not perfect, but as this only affects test and dev mode and not production it is better
     * than nothing.
     * <p>
     * Really we should just not use the common pool at all.
     * <p>
     * TODO: This no longer works in Java 25 because the `ForkJoinPool` now does
     * <a href=
     * "https://github.com/openjdk/jdk/blob/jdk-25%2B36/src/java.base/share/classes/java/util/concurrent/ForkJoinPool.java#L2085">
     * this</a>, which ends up calling <a href=
     * "https://github.com/openjdk/jdk/blob/jdk-25%2B36/src/java.base/share/classes/java/util/concurrent/ForkJoinWorkerThread.java#L280">this</a>.
     * We need to figure out how to deal with this
     */
    public static void setForkJoinClassLoader(ClassLoader classLoader) {
        CountDownLatch allDone = new CountDownLatch(ForkJoinPool.getCommonPoolParallelism());
        CountDownLatch taskRelease = new CountDownLatch(1);
        for (int i = 0; i < ForkJoinPool.getCommonPoolParallelism(); ++i) {
            ForkJoinPool.commonPool().execute(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    allDone.countDown();
                    try {
                        taskRelease.await();
                    } catch (InterruptedException e) {
                        log.error("Failed to set fork join ClassLoader", e);
                    }
                }
            });
        }
        try {
            if (!allDone.await(1, TimeUnit.SECONDS)) {
                log.error(
                        "Timed out trying to set fork join ClassLoader, this should never happen unless something has tied up a fork join thread before the app launched");
            }
        } catch (InterruptedException e) {
            log.error("Failed to set fork join ClassLoader", e);
        } finally {
            taskRelease.countDown();
        }
    }
}
