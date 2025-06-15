package io.quarkus.gradle.tasks;

import static java.lang.ProcessBuilder.Redirect.PIPE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.gradle.api.logging.Logger;

public class ProcessUtil {
    private static class ProcessReader implements Runnable {

        private final InputStream inputStream;

        private ProcessReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            byte[] b = new byte[100];
            int i;
            try {
                while ((i = inputStream.read(b)) > 0) {
                    System.out.print(new String(b, 0, i, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void launch(List<String> args, File wdir, Logger logger) {
        // this was all very touchy to get the process outputing to console and exiting cleanly
        // change at your own risk

        // We cannot use getProject().exec() as contrl-c is not processed correctly
        // and the spawned process will not shutdown
        //
        // This also requires running with --no-daemon as control-c doesn't seem to trigger the shutdown hook
        // this poor gradle behavior is a long known issue with gradle

        try {
            ProcessBuilder builder = new ProcessBuilder().command(args).redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(PIPE).redirectOutput(PIPE).directory(wdir);
            Process process = builder.start();

            ProcessReader error = new ProcessReader(process.getErrorStream());
            ProcessReader stdout = new ProcessReader(process.getInputStream());
            Thread t = new Thread(error, "Error stream reader");
            t.start();
            t = new Thread(stdout, "Stdout stream reader");
            t.start();

            Runtime.getRuntime().addShutdownHook(new Thread("Quarkus Run Process Shutdown") {

                @Override
                public void run() {
                    ProcessUtil.destroyProcess(process, true);
                }
            });

            try {
                process.waitFor();
            } finally {
                ProcessUtil.destroyProcess(process, true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void destroyProcess(Process quarkusProcess) {
        quarkusProcess.destroy();
        int i = 0;
        while (i++ < 10) {
            try {
                Thread.sleep(LOG_CHECK_INTERVAL);
            } catch (InterruptedException ignored) {

            }
            if (!quarkusProcess.isAlive()) {
                break;
            }
        }

        if (quarkusProcess.isAlive()) {
            quarkusProcess.destroyForcibly();
        }
    }

    public static final int LOG_CHECK_INTERVAL = 500;

    public static void destroyProcess(ProcessHandle quarkusProcess) {
        try {
            CompletableFuture<ProcessHandle> exit = quarkusProcess.onExit();
            if (!quarkusProcess.destroy()) {
                quarkusProcess.destroyForcibly();
                return;
            }
            exit.get(LOG_CHECK_INTERVAL * 10, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
        if (quarkusProcess.isAlive()) {
            quarkusProcess.destroyForcibly();
        }
    }

    public static void destroyProcess(Process process, boolean children) {
        if (!children) {
            destroyProcess(process);
            return;
        }
        process.descendants().forEach((p) -> destroyProcess(p));
        destroyProcess(process);
    }

}
