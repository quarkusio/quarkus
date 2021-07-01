package io.quarkus.cli.build;

import static picocli.CommandLine.ExitCode.OK;
import static picocli.CommandLine.ExitCode.SOFTWARE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.utilities.OS;

public class ExecuteUtil {

    public static File findExecutableFile(String base) {
        String path = null;
        String executable = base;

        if (OS.determineOS() == OS.WINDOWS) {
            executable = base + ".cmd";
            path = findExecutable(executable);
            if (path == null) {
                executable = base + ".bat";
                path = findExecutable(executable);
            }
        } else {
            executable = base;
            path = findExecutable(executable);
        }
        if (path == null)
            return null;
        return new File(path, executable);
    }

    private static String findExecutable(String exec) {
        return Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator))).map(Paths::get)
                .map(path -> path.resolve(exec).toFile()).filter(File::exists).findFirst().map(File::getParent)
                .orElse(null);
    }

    public static File findExecutable(String name, String errorMessage, OutputOptionMixin output) {
        File command = ExecuteUtil.findExecutableFile(name);
        if (command == null) {
            output.error(errorMessage);
            throw new RuntimeException("Unable to find " + name + " command");
        }
        return command;
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
            Process process = new ProcessBuilder()
                    .command(args)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .directory(parentDir)
                    .start();

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
            Process process = new ProcessBuilder()
                    .command(args)
                    .inheritIO()
                    .directory(parentDir)
                    .start();
            exit = process.waitFor();
        }

        if (exit != 0) {
            return SOFTWARE;
        } else {
            return OK;
        }
    }

    public static File findWrapper(Path projectRoot, String[] windows, String other) {
        if (OS.determineOS() == OS.WINDOWS) {
            for (String name : windows) {
                File wrapper = new File(projectRoot + File.separator + name);
                if (wrapper.isFile())
                    return wrapper;
            }
        } else {
            File wrapper = new File(projectRoot + File.separator + other);
            if (wrapper.isFile())
                return wrapper;
        }

        return null;
    }
}
