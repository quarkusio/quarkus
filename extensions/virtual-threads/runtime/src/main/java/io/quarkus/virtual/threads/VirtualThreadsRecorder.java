package io.quarkus.virtual.threads;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

@Recorder
public class VirtualThreadsRecorder {

    private static final Logger logger = Logger.getLogger("io.quarkus.virtual-threads");

    static VirtualThreadsConfig config = new VirtualThreadsConfig();

    private static volatile Executor current;
    private static final Object lock = new Object();

    public void setupVirtualThreads(VirtualThreadsConfig c, ShutdownContext shutdownContext, LaunchMode launchMode) {
        config = c;
        if (launchMode == LaunchMode.DEVELOPMENT) {
            shutdownContext.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    Executor executor = current;
                    if (executor instanceof ExecutorService) {
                        ((ExecutorService) executor).shutdownNow();
                    }
                    current = null;
                }
            });
        } else {
            shutdownContext.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    Executor executor = current;
                    if (executor instanceof ExecutorService) {
                        ExecutorService service = (ExecutorService) executor;
                        service.shutdown();

                        final long timeout = config.shutdownTimeout.toNanos();
                        final long interval = config.shutdownCheckInterval.orElse(config.shutdownTimeout).toNanos();

                        long start = System.nanoTime();
                        int loop = 1;
                        long elapsed = 0;
                        for (;;) {
                            // This log can be very useful when debugging problems
                            logger.debugf("Await termination loop: %s, remaining: %s", loop++, timeout - elapsed);
                            try {
                                if (!service.awaitTermination(Math.min(timeout, interval), NANOSECONDS)) {
                                    elapsed = System.nanoTime() - start;
                                    if (elapsed >= timeout) {
                                        service.shutdownNow();
                                        break;
                                    }
                                } else {
                                    return;
                                }
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }
                }
            });
        }
    }

    public static Executor getCurrent() {
        Executor executor = current;
        if (executor != null) {
            return executor;
        }
        synchronized (lock) {
            if (current == null) {
                current = createExecutor();
            }
            return current;
        }
    }

    static ExecutorService newVirtualThreadPerTaskExecutorWithName(String prefix)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Method ofVirtual = Thread.class.getMethod("ofVirtual");
        Object vtb = ofVirtual.invoke(VirtualThreadsRecorder.class);
        Class<?> vtbClass = Class.forName("java.lang.Thread$Builder$OfVirtual");
        Method name = vtbClass.getMethod("name", String.class, long.class);
        vtb = name.invoke(vtb, prefix, 0);
        Method factory = vtbClass.getMethod("factory");
        ThreadFactory tf = (ThreadFactory) factory.invoke(vtb);

        return (ExecutorService) Executors.class.getMethod("newThreadPerTaskExecutor", ThreadFactory.class)
                .invoke(VirtualThreadsRecorder.class, tf);
    }

    static ExecutorService newVirtualThreadPerTaskExecutor()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (ExecutorService) Executors.class.getMethod("newVirtualThreadPerTaskExecutor")
                .invoke(VirtualThreadsRecorder.class);
    }

    static ExecutorService newVirtualThreadExecutor()
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        try {
            Optional<String> namePrefix = config.namePrefix;
            return namePrefix.isPresent() ? newVirtualThreadPerTaskExecutorWithName(namePrefix.get())
                    : newVirtualThreadPerTaskExecutor();
        } catch (ClassNotFoundException e) {
            logger.warn("Unable to invoke java.util.concurrent.Executors#newThreadPerTaskExecutor" +
                    " with VirtualThreadFactory, falling back to unnamed virtual threads", e);
            return newVirtualThreadPerTaskExecutor();
        }
    }

    /**
     * This method uses reflection in order to allow developers to quickly test quarkus-loom without needing to
     * change --release, --source, --target flags and to enable previews.
     * Since we try to load the "Loom-preview" classes/methods at runtime, the application can even be compiled
     * using java 11 and executed with a loom-compliant JDK.
     */
    private static Executor createExecutor() {
        try {
            return new ContextPreservingExecutorService(newVirtualThreadExecutor());
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            logger.debug("Unable to invoke java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor", e);
            //quite ugly but works
            logger.warn("You weren't able to create an executor that spawns virtual threads, the default" +
                    " blocking executor will be used, please check that your JDK is compatible with " +
                    "virtual threads");
            //if for some reason a class/method can't be loaded or invoked we return the traditional executor,
            // wrapping executeBlocking.
            return new Executor() {
                @Override
                public void execute(Runnable command) {
                    var context = Vertx.currentContext();
                    if (!(context instanceof ContextInternal)) {
                        Infrastructure.getDefaultWorkerPool().execute(command);
                    } else {
                        context.executeBlocking(fut -> {
                            try {
                                command.run();
                                fut.complete(null);
                            } catch (Exception e) {
                                fut.fail(e);
                            }
                        }, false);
                    }
                }
            };
        }
    }

}
