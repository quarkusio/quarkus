package io.quarkus.runtime;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.threads.EnhancedQueueExecutor;

import sun.misc.Unsafe;

/**
 * An executor that can be 'cleaned'. Each clean increments an internal generation number. When a task is executed by
 * a thread that has a different generation number to the last task executed by the thread then the threads ThreadLocal's
 * will be cleared. This means that thread locals from a previous deployment cannot interfere with the current deployment.
 *
 * This is only for development mode, it must not be used for production applications.
 *
 * TODO: should this just provide a facacde that simply starts a new thread pool instead?
 */
public final class CleanableExecutor implements ExecutorService {

    private final EnhancedQueueExecutor executor;

    private static final AtomicInteger generation = new AtomicInteger(1);
    private final ThreadLocal<Integer> lastGeneration = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return -1;
        }
    };

    public CleanableExecutor(EnhancedQueueExecutor executor) {
        this.executor = executor;
    }

    public void clean() {
        //also clear the current thread, as this is called by the hot deployment thread so it is about to hit a new deployment
        Resetter.run();
        generation.incrementAndGet();
        CountDownLatch latch = new CountDownLatch(1);
        Runnable empty = new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        //this is a hack, basically just submit a heap of tasks to the pool in order to try and clean up all
        //threadlocal state. It does not really matter if this does not work, however if these threads are holding
        //state that is stopping things being GC'ed it can help with memory usage
        try {
            for (int i = 0; i < executor.getMaximumPoolSize(); ++i) {
                submit(empty);
            }
        } finally {
            latch.countDown();
        }

    }

    private void handleClean(int taskGen) {
        int val = lastGeneration.get();
        if (val == -1) {
            lastGeneration.set(taskGen);
        } else if (val != taskGen) {
            Resetter.run();
            lastGeneration.set(taskGen);
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(new CleaningCallable<>(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(new CleaningRunnable(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(new CleaningRunnable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        ArrayList<Callable<T>> submit = createWrappedList(tasks);
        return executor.invokeAll(submit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        ArrayList<Callable<T>> submit = createWrappedList(tasks);
        return executor.invokeAll(submit, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        ArrayList<Callable<T>> submit = createWrappedList(tasks);
        return executor.invokeAny(submit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        ArrayList<Callable<T>> submit = createWrappedList(tasks);
        return executor.invokeAny(submit, timeout, unit);
    }

    private <T> ArrayList<Callable<T>> createWrappedList(Collection<? extends Callable<T>> tasks) {
        ArrayList<Callable<T>> submit = new ArrayList<>();
        for (Callable<T> i : tasks) {
            submit.add(new CleaningCallable<>(i));
        }
        return submit;
    }

    @Override
    public void execute(Runnable command) {
        executor.submit(new CleaningRunnable(command));
    }

    /**
     * copied from JBoss threads
     */
    static final class Resetter {
        private static final long threadLocalMapOffs;
        private static final long inheritableThreadLocalMapOffs;

        static {
            final Field threadLocals = AccessController.doPrivileged(new DeclaredFieldAction(Thread.class, "threadLocals"));
            threadLocalMapOffs = threadLocals == null ? 0 : unsafe.objectFieldOffset(threadLocals);
            final Field inheritableThreadLocals = AccessController
                    .doPrivileged(new DeclaredFieldAction(Thread.class, "inheritableThreadLocals"));
            inheritableThreadLocalMapOffs = inheritableThreadLocals == null ? 0
                    : unsafe.objectFieldOffset(inheritableThreadLocals);
        }

        static void run() {
            final Thread thread = Thread.currentThread();
            if (threadLocalMapOffs != 0)
                unsafe.putObject(thread, threadLocalMapOffs, null);
            if (inheritableThreadLocalMapOffs != 0)
                unsafe.putObject(thread, inheritableThreadLocalMapOffs, null);
        }
    }

    static final Unsafe unsafe;

    static {
        unsafe = AccessController.doPrivileged(new PrivilegedAction<Unsafe>() {
            public Unsafe run() {
                try {
                    final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                    field.setAccessible(true);
                    return (Unsafe) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (NoSuchFieldException e) {
                    throw new NoSuchFieldError(e.getMessage());
                }
            }
        });
    }

    static final class DeclaredFieldAction implements PrivilegedAction<Field> {
        private final Class<?> clazz;
        private final String fieldName;

        DeclaredFieldAction(final Class<?> clazz, final String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        public Field run() {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
    }

    private class CleaningRunnable implements Runnable {
        private final Runnable command;
        final int gen = generation.get();

        public CleaningRunnable(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            handleClean(gen);
            command.run();
        }
    }

    private class CleaningCallable<T> implements Callable<T> {
        private final Callable<T> i;
        final int gen = generation.get();

        public CleaningCallable(Callable<T> i) {
            this.i = i;
        }

        @Override
        public T call() throws Exception {
            handleClean(gen);
            return i.call();
        }
    }
}
