
package io.quarkus.deployment.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.jboss.logging.Logger;

public class ExecUtil {

    private static final Logger LOG = Logger.getLogger(ExecUtil.class);

    public static final Function<InputStream, Runnable> INFO_LOGGING = i -> new HandleOutput(i, Logger.Level.INFO);
    public static final Function<InputStream, Runnable> DEBUG_LOGGING = i -> new HandleOutput(i, Logger.Level.DEBUG);
    public static final Function<InputStream, Runnable> SYSTEM_LOGGING = HandleOutput::new;

    private static final Function<InputStream, Runnable> DEFAULT_LOGGING = INFO_LOGGING;

    private static final int PROCESS_CHECK_INTERVAL = 500;

    public static class HandleOutput implements Runnable {

        private final InputStream is;
        private final Optional<Logger.Level> logLevel;
        private final Logger logger;

        public HandleOutput(InputStream is) {
            this(is, null);
        }

        public HandleOutput(InputStream is, Logger.Level logLevel) {
            this(is, logLevel, LOG);
        }

        public HandleOutput(InputStream is, Logger.Level logLevel, Logger logger) {
            this.is = is;
            this.logLevel = Optional.ofNullable(logLevel);
            this.logger = logger;
        }

        @Override
        public void run() {
            try (InputStreamReader isr = new InputStreamReader(is); BufferedReader reader = new BufferedReader(isr)) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    final String l = line;
                    logLevel.ifPresentOrElse(level -> logger.log(level, l), () -> System.out.println(l));
                }
            } catch (IOException e) {
                logLevel.ifPresentOrElse(level -> logger.log(level, "Failed to handle output", e), () -> e.printStackTrace());
            }
        }
    }

    /**
     * Execute the specified command from within the current directory.
     *
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean exec(String command, String... args) {
        return exec(new File("."), command, args);
    }

    /**
     * Execute the specified command until the given timeout from within the current directory.
     *
     * @param timeout The timeout
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execWithTimeout(Duration timeout, String command, String... args) {
        return execWithTimeout(new File("."), timeout, command, args);
    }

    /**
     * Execute the specified command from within the specified directory.
     *
     * @param directory The directory
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean exec(File directory, String command, String... args) {
        return exec(directory, DEFAULT_LOGGING, command, args);
    }

    /**
     * Execute the specified command until the given timeout from within the specified directory.
     *
     * @param directory The directory
     * @param timeout The timeout
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execWithTimeout(File directory, Duration timeout, String command, String... args) {
        return execWithTimeout(directory, DEFAULT_LOGGING, timeout, command, args);
    }

    /**
     * Execute the specified command from within the specified directory.
     * The method allows specifying an output filter that processes the command output.
     *
     * @param directory The directory
     * @param outputFilterFunction A {@link Function} that gets an {@link InputStream} and returns an outputFilter.
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean exec(File directory, Function<InputStream, Runnable> outputFilterFunction, String command,
            String... args) {
        return execProcess(startProcess(directory, command, args), outputFilterFunction) == 0;
    }

    public static int execProcess(Process process, Function<InputStream, Runnable> outputFilterFunction) {
        try {
            Function<InputStream, Runnable> loggingFunction = outputFilterFunction != null ? outputFilterFunction
                    : DEFAULT_LOGGING;
            Thread t = new Thread(loggingFunction.apply(process.getInputStream()));
            t.setName("Process stdout");
            t.setDaemon(true);
            t.start();
            process.waitFor();
            return process.exitValue();
        } catch (InterruptedException e) {
            return -1;
        } finally {
            destroyProcess(process);
        }
    }

    public static int execProcessWithTimeout(Process process, Function<InputStream, Runnable> outputFilterFunction,
            Duration timeout) {
        try {
            Function<InputStream, Runnable> loggingFunction = outputFilterFunction != null ? outputFilterFunction
                    : DEFAULT_LOGGING;
            Thread t = new Thread(loggingFunction.apply(process.getInputStream()));
            t.setName("Process stdout");
            t.setDaemon(true);
            t.start();
            process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return process.exitValue();
        } catch (InterruptedException e) {
            return -1;
        } finally {
            destroyProcess(process);
        }
    }

    /**
     * Execute the specified command until the given timeout from within the specified directory.
     * The method allows specifying an output filter that processes the command output.
     *
     * @param directory The directory
     * @param outputFilterFunction A {@link Function} that gets an {@link InputStream} and returns an outputFilter.
     * @param timeout The timeout
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execWithTimeout(File directory, Function<InputStream, Runnable> outputFilterFunction,
            Duration timeout, String command, String... args) {
        return execProcessWithTimeout(startProcess(directory, command, args), outputFilterFunction, timeout) == 0;
    }

    /**
     * Execute the specified command from within the current directory using debug logging.
     *
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execWithDebugLogging(String command, String... args) {
        return execWithDebugLogging(new File("."), command, args);
    }

    /**
     * Execute the specified command from within the specified directory using debug logging.
     *
     * @param directory The directory
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execWithDebugLogging(File directory, String command, String... args) {
        return exec(directory, DEBUG_LOGGING, command, args);
    }

    /**
     * Execute the specified command from within the current directory using system logging.
     *
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execWithSystemLogging(String command, String... args) {
        return execWithSystemLogging(new File("."), command, args);
    }

    /**
     * Execute the specified command from within the specified directory using system logging.
     *
     * @param directory The directory
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execWithSystemLogging(File directory, String command, String... args) {
        return exec(directory, SYSTEM_LOGGING, command, args);
    }

    /**
     * Start a process executing given command with arguments within the specified directory.
     *
     * @param directory The directory
     * @param command The command and args
     * @param environment The command environment
     * @return the process
     */
    public static Process startProcess(File directory, Map<String, String> environment, String command, String... args) {
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = command;
            if (args.length > 0) {
                System.arraycopy(args, 0, cmd, 1, args.length);
            }
            ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory(directory)
                    .command(cmd)
                    .redirectErrorStream(true);
            if (environment != null && !environment.isEmpty()) {
                Map<String, String> env = processBuilder.environment();
                environment.forEach((key, value) -> {
                    if (value == null) {
                        env.remove(key);
                    } else {
                        env.put(key, value);
                    }
                });
            }
            return processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Input/Output error while executing command.", e);
        }
    }

    /**
     * Start a process executing given command with arguments within the specified directory.
     *
     * @param directory The directory
     * @param command The command
     * @param args The command arguments
     * @return the process
     */
    public static Process startProcess(File directory, String command, String... args) {
        return startProcess(directory, Collections.emptyMap(), command, args);
    }

    /**
     * Kill the process, if still alive, kill it forcibly
     *
     * @param process the process to kill
     */
    public static void destroyProcess(Process process) {
        process.destroy();
        int i = 0;
        while (process.isAlive() && i++ < 10) {
            try {
                process.waitFor(PROCESS_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }
}
