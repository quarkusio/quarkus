package io.quarkus.runtime.init;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractPreInitRunner {

    private static final AtomicInteger THREAD_ID = new AtomicInteger();

    protected static void doExecutePreInitTasks(Runnable[] preInitTasks) {
        // 5 came empirically: it is both fast enough and low overhead
        int threadCount = Math.min(5, Runtime.getRuntime().availableProcessors());

        ExecutorService executor = Executors.newFixedThreadPool(
                threadCount,
                new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread t = new Thread(runnable, "pre-init-" + THREAD_ID.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                });

        CountDownLatch latch = new CountDownLatch(preInitTasks.length);

        for (Runnable preInitTask : preInitTasks) {
            submit(executor, latch, preInitTask);
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    executor.shutdown();
                }
            }
        });
    }

    private static void submit(ExecutorService executor,
            CountDownLatch latch,
            Runnable task) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            }
        });
    }
}
