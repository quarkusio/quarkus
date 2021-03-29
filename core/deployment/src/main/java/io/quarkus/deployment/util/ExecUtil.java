
package io.quarkus.deployment.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

import org.jboss.logging.Logger;

public class ExecUtil {

    private static final Logger LOG = Logger.getLogger(ExecUtil.class);

    private static final Function<InputStream, Runnable> PRINT_OUTPUT = i -> new HandleOutput(i);
    private static final Function<InputStream, Runnable> SILENT = i -> new HandleOutput(i, Logger.Level.DEBUG);

    private static class HandleOutput implements Runnable {

        private final InputStream is;
        private final Logger.Level logLevel;

        HandleOutput(InputStream is) {
            this(is, Logger.Level.INFO);
        }

        HandleOutput(InputStream is, Logger.Level logLevel) {
            this.is = is;
            this.logLevel = LOG.isEnabled(logLevel) ? logLevel : null;
        }

        @Override
        public void run() {
            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (logLevel != null) {
                        LOG.log(logLevel, line);
                    }
                }
            } catch (IOException e) {
                if (logLevel != null) {
                    LOG.log(logLevel, "Failed to handle output", e);
                }
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
     * Execute the specified command from within the current directory and hide the output.
     *
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execSilent(String command, String... args) {
        return execSilent(new File("."), command, args);
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
        return exec(directory, PRINT_OUTPUT, command, args);
    }

    /**
     * Execute the specified command from within the specified directory and hide the output.
     *
     * @param directory The directory
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean execSilent(File directory, String command, String... args) {
        return exec(directory, SILENT, command, args);
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
        Process process = null;
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = command;
            if (args.length > 0) {
                System.arraycopy(args, 0, cmd, 1, args.length);
            }
            process = new ProcessBuilder()
                    .directory(directory)
                    .command(cmd)
                    .redirectErrorStream(true)
                    .start();

            outputFilterFunction.apply(process.getInputStream()).run();
            process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("Input/Output error while executing command.", e);
        } catch (InterruptedException e) {
            return false;
        }
        return process != null && process.exitValue() == 0;
    }

}
