package io.quarkus.runtime;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

public class StartupContext implements Closeable {

    public static final String RAW_COMMAND_LINE_ARGS = StartupContext.class.getName() + ".raw-command-line-args";

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    // Holds values for returned proxies
    // These values are usually returned from recorder methods but can be also set explicitly
    // For example, the raw command line args and ShutdownContext are set when the StartupContext is created
    private final Map<String, Object> values = new HashMap<>();

    private final PriorityBlockingQueue<ShutdownTask> shutdownTasks = new PriorityBlockingQueue<>();
    private String[] commandLineArgs;
    private String currentBuildStepName;

    public StartupContext() {
        ShutdownContext shutdownContext = new ShutdownContext() {
            @Override
            public void addShutdownTask(Priority priority, Runnable runnable) {
                if (runnable != null && priority != null) {
                    shutdownTasks.offer(new ShutdownTask(priority.value(), runnable));
                } else {
                    throw new IllegalArgumentException("Extension passed an invalid shutdown priority/handler");
                }
            }
        };
        values.put(ShutdownContext.class.getName(), shutdownContext);
        values.put(RAW_COMMAND_LINE_ARGS, new Supplier<String[]>() {
            @Override
            public String[] get() {
                if (commandLineArgs == null) {
                    throw new RuntimeException("Command line arguments not available during static init");
                }
                return commandLineArgs;
            }
        });
    }

    public void putValue(String name, Object value) {
        values.put(name, value);
    }

    public Object getValue(String name) {
        return values.get(name);
    }

    @Override
    public void close() {
        runAllAndClear(shutdownTasks);
        values.clear();
    }

    private void runAllAndClear(Queue<ShutdownTask> tasks) {
        while (!tasks.isEmpty()) {
            try {
                var runnable = tasks.remove();
                runnable.run();
            } catch (Throwable ex) {
                LOG.error("Running a shutdown task failed", ex);
            }
        }
    }

    @SuppressWarnings("unused")
    public void setCommandLineArguments(String[] commandLineArguments) {
        this.commandLineArgs = commandLineArguments;
    }

    @SuppressWarnings("unused")
    public String getCurrentBuildStepName() {
        return currentBuildStepName;
    }

    @SuppressWarnings("unused")
    public void setCurrentBuildStepName(String currentBuildStepName) {
        this.currentBuildStepName = currentBuildStepName;
    }

    private record ShutdownTask(int priority, Runnable action) implements Runnable, Comparable<ShutdownTask> {

        @Override
        public int compareTo(ShutdownTask task) {
            // with a regular int compare we'll get the asc ordered queue [1,2,3,4]
            //  but we want the desc order [4,3,2,1] so that the task with the higher priority is polled first:
            return task == null ? 1 : -Integer.compare(priority, task.priority);
        }

        @Override
        public void run() {
            action.run();
        }
    }
}
