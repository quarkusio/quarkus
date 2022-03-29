package io.quarkus.runtime.shutdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

public class ShutdownHooks {

    private static final Logger LOG = Logger.getLogger(ShutdownHooks.class);

    private static volatile List<Runnable> shutdownHooks = new ArrayList<>();

    public static void registerShutdownHook(Runnable runnable) {
        shutdownHooks.add(new ShutdownHook(runnable));
    }

    public static void registerShutdownHook(Runnable runnable, long timeout, TimeUnit unit) {
        shutdownHooks.add(new ShutdownHook(runnable, timeout, unit));
    }

    public static void runShutdown() {
        List<Runnable> toClose = new ArrayList<>(shutdownHooks);
        Collections.reverse(toClose);
        for (Runnable r : toClose) {
            r.run();
        }
    }

    private static class ShutdownHook implements Runnable {

        private final Runnable runnable;
        private long timeout = -1L;
        private TimeUnit unit = TimeUnit.MILLISECONDS;

        public ShutdownHook(Runnable runnable) {
            this.runnable = runnable;
        }

        private ShutdownHook(Runnable runnable, long timeout, TimeUnit unit) {
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout value is negative");
            }
            if (unit == null) {
                throw new IllegalArgumentException("unit has no value");
            }
            this.runnable = runnable;
            this.timeout = timeout;
            this.unit = unit;
        }

        public void run() {
            try {
                Thread t = new Thread(runnable);
                t.setDaemon(false);
                t.start();
                if (timeout >= 0) {
                    t.join(unit.toMillis(timeout));
                } else {
                    t.join();
                }
            } catch (Throwable e) {
                LOG.error("Running a shutdown task failed", e);
            }
        }
    }
}
