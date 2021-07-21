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
     * 
     * It's not perfect, but as this only affects test and dev mode and not production it is better
     * than nothing.
     * 
     * Really we should just not use the common pool at all.
     */
    public static void setForkJoinClassLoader(ClassLoader classLoader) {
        final ForkJoinPool commonPool = ForkJoinPool.commonPool();

        final int activeThreadCount = commonPool.getActiveThreadCount();

        //The following approach is rather heavy, but there's good chances that there are actually no active threads,
        //so skip it in this case.
        //Additionally, if we can skip this this will prevent us from actually forcing to start all threads in the pool.
        if (activeThreadCount > 0) {
            //TODO we could theoretically improve this by using the system property java.util.concurrent.ForkJoinPool.common.threadFactory
            //and force the common pool to register created threads so that we can reach and reset them.
            CountDownLatch allDone = new CountDownLatch(ForkJoinPool.getCommonPoolParallelism());
            CountDownLatch taskRelease = new CountDownLatch(1);
            for (int i = 0; i < ForkJoinPool.getCommonPoolParallelism(); ++i) {
                commonPool.execute(new Runnable() {
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
}
