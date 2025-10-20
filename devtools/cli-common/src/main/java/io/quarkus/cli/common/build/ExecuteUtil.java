package io.quarkus.cli.common.build;

import static picocli.CommandLine.ExitCode.OK;
import static picocli.CommandLine.ExitCode.SOFTWARE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

        var holder = new Object() {
            int exitCode;
        };
        io.smallrye.common.process.ProcessBuilder.Input<Void> pb = io.smallrye.common.process.ProcessBuilder.newBuilder(args[0])
                .arguments(List.of(args).subList(1, args.length))
                .directory(parentDir.toPath())
                .exitCodeChecker(ec -> {
                    holder.exitCode = ec;
                    return true;
                })
                .softExitTimeout(null)
                .hardExitTimeout(null)
                .input().inherited();
        if (output.isCliTest()) {
            // We have to capture IO differently in tests..
            pb.output().consumeWith(br -> br.lines().forEach(output.out()::println))
                    .error().consumeWith(br -> br.lines().forEach(output.err()::println))
                    .run();
        } else {
            pb.output().inherited()
                    .error().inherited()
                    .run();
        }

        if (holder.exitCode != 0) {
            return SOFTWARE;
        } else {
            return OK;
        }
    }

    public static File findWrapper(Path projectRoot, String[] windows, String other) {
        return Executable.findWrapper(projectRoot, windows, other);
    }
}
