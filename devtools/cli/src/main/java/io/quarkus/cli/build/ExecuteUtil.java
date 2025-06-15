package io.quarkus.cli.build;

import static picocli.CommandLine.ExitCode.OK;
import static picocli.CommandLine.ExitCode.SOFTWARE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.devtools.exec.ExecSupport;
import io.quarkus.devtools.exec.Executable;

public class ExecuteUtil {

    private static ExecSupport withOutput(OutputOptionMixin output) {
        return new ExecSupport(output.out(), output.err(), output.isVerbose(), output.isCliTest());
    }

    public static File findExecutableFile(String base) {
        return Executable.findExecutableFile(base);
    }

    private static String findExecutable(String exec) {
        return Executable.findExecutable(exec);
    }

    public static File findExecutable(String name, String errorMessage, OutputOptionMixin output) {
        return Executable.findExecutable(name, errorMessage, output);
    }

    public static int executeProcess(OutputOptionMixin output, String[] args, File parentDir)
            throws IOException, InterruptedException {
        if (output.isVerbose()) {
            output.out().println(String.join(" ", args));
            output.out().println();
        }

        int exit = SOFTWARE;
        if (output.isCliTest()) {
            // We have to capture IO differently in tests..
            Process process = new ProcessBuilder().command(args).redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .directory(parentDir).start();

            // Drain the output/errors streams
            ExecutorService service = Executors.newFixedThreadPool(2);
            service.submit(() -> {
                new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                        .forEach(output.out()::println);
            });
            service.submit(() -> {
                new BufferedReader(new InputStreamReader(process.getErrorStream())).lines()
                        .forEach(output.err()::println);
            });
            process.waitFor(5, TimeUnit.MINUTES);
            service.shutdown();

            exit = process.exitValue();
        } else {
            Process process = new ProcessBuilder().command(args).inheritIO().directory(parentDir).start();
            exit = process.waitFor();
        }

        if (exit != 0) {
            return SOFTWARE;
        } else {
            return OK;
        }
    }

    public static File findWrapper(Path projectRoot, String[] windows, String other) {
        return Executable.findWrapper(projectRoot, windows, other);
    }
}
