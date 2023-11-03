package io.quarkus.dev.io;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * We need to be able to update the TCCL of the NIO default thread pool in dev/test mode
 * <p>
 * This class lets us to that.
 */
public class NioThreadPoolThreadFactory implements ThreadFactory {

    private static final CopyOnWriteArrayList<Thread> allThreads = new CopyOnWriteArrayList<>();
    private static volatile ClassLoader currentCl = Thread.currentThread().getContextClassLoader();

    @Override
    public Thread newThread(Runnable r) {
        AtomicReference<Thread> t = new AtomicReference<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } finally {
                    allThreads.remove(t.get());
                }
            }
        }, "NIO IO Thread (created by Quarkus)");
        t.set(thread);
        thread.setDaemon(true);
        allThreads.add(thread);
        thread.setContextClassLoader(currentCl);
        return thread;
    }

    public static ClassLoader updateTccl(ClassLoader cl) {
        ClassLoader old = currentCl;
        currentCl = cl;
        for (Thread i : allThreads) {
            i.setContextClassLoader(cl);
        }
        return old;
    }
}
