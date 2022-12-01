package io.quarkus.deployment.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

/**
 * Utility for {@link Process} related operations
 */
public class ProcessUtil {

    private static final Logger logger = Logger.getLogger(ProcessUtil.class);

    /**
     * Launches and returns a {@link Process} built from the {@link ProcessBuilder builder}.
     * Before launching the process, this method checks if inherit IO is disabled and if so,
     * streams both the {@code STDOUT} and {@code STDERR} of the launched process using
     * {@link #streamToSysOutSysErr(Process)}. Else, it launches the process with {@link ProcessBuilder#inheritIO()}
     *
     * @param builder The process builder
     * @param shouldRedirectIO Whether {@link java.lang.ProcessBuilder.Redirect#INHERIT} can be used for
     *        launching the process
     * @return Returns the newly launched process
     * @throws IOException
     */
    public static Process launchProcess(final ProcessBuilder builder,
            final boolean shouldRedirectIO) throws IOException {
        if (!shouldRedirectIO) {
            return builder.inheritIO().start();
        }
        final Process process = builder.redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();
        // stream both stdout and stderr of the process
        ProcessUtil.streamToSysOutSysErr(process);
        return process;
    }

    /**
     * Launches and returns a {@link Process} built from the {@link ProcessBuilder builder}.
     * Before launching the process, this method checks if inheritIO is disabled
     * and if so, streams (only) the {@code STDOUT} of the launched process using {@link #streamOutputToSysOut(Process)}
     * (Process)}. Else, it launches the process with {@link ProcessBuilder#inheritIO()}
     *
     * @param builder The process builder
     * @param shouldRedirectIO Whether {@link java.lang.ProcessBuilder.Redirect#INHERIT} can be used for
     *        launching the process
     * @return Returns the newly launched process
     * @throws IOException
     */
    public static Process launchProcessStreamStdOut(final ProcessBuilder builder,
            boolean shouldRedirectIO) throws IOException {
        if (!shouldRedirectIO) {
            return builder.inheritIO().start();
        }
        final Process process = builder.redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();
        // stream only stdout of the process
        ProcessUtil.streamOutputToSysOut(process);
        return process;
    }

    /**
     * This is a convenience method which internally calls both the {@link #streamOutputToSysOut(Process)}
     * and {@link #streamErrorToSysErr(Process)} methods
     *
     * @param process The process whose STDOUT and STDERR needs to be streamed.
     */
    public static void streamToSysOutSysErr(final Process process) {
        streamOutputToSysOut(process);
        streamErrorToSysErr(process);
    }

    /**
     * Streams the {@link Process process'} {@code STDOUT} to the current process'
     * {@code System.out stream}. This creates and starts a thread to stream the contents.
     * The {@link Process} is expected to have been started in {@link java.lang.ProcessBuilder.Redirect#PIPE}
     * mode
     *
     * @param process The process whose STDOUT needs to be streamed.
     */
    public static void streamOutputToSysOut(final Process process) {
        final InputStream processStdOut = process.getInputStream();
        final Thread t = new Thread(new Streamer(processStdOut, System.out));
        t.setName("Process stdout streamer");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Streams the {@link Process process'} {@code STDERR} to the current process'
     * {@code System.err stream}. This creates and starts a thread to stream the contents.
     * The {@link Process} is expected to have been started in {@link java.lang.ProcessBuilder.Redirect#PIPE}
     * mode
     *
     * @param process The process whose STDERR needs to be streamed.
     */
    public static void streamErrorToSysErr(final Process process) {
        streamErrorTo(System.err, process);
    }

    /**
     * Streams the {@link Process process'} {@code STDERR} to the given
     * {@code printStream}. This creates and starts a thread to stream the contents.
     * The {@link Process} is expected to have been started in {@link java.lang.ProcessBuilder.Redirect#PIPE}
     * mode
     *
     * @param process The process whose STDERR needs to be streamed.
     */
    public static void streamErrorTo(final PrintStream printStream, final Process process) {
        final InputStream processStdErr = process.getErrorStream();
        final Thread t = new Thread(new Streamer(processStdErr, printStream));
        t.setName("Process stderr streamer");
        t.setDaemon(true);
        t.start();
    }

    private static final class Streamer implements Runnable {

        private final InputStream processStream;
        private final PrintStream consumer;

        private Streamer(final InputStream processStream, final PrintStream consumer) {
            this.processStream = processStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(processStream, StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    consumer.println(line);
                }
            } catch (IOException e) {
                logger.debug("Ignoring exception that occurred during streaming of " + processStream, e);
            }
        }
    }
}
