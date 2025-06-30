package io.quarkus.devtools.exec;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import io.smallrye.common.process.ProcessBuilder;

public class ExecSupport {

    private final PrintWriter out;
    private final PrintWriter err;

    private final boolean verbose;
    private final boolean cliTest;

    public ExecSupport() {
        this(System.out, System.err, false, false);
    }

    public ExecSupport(PrintStream out, PrintStream err, boolean verbose, boolean cliTest) {
        this(new PrintWriter(out), new PrintWriter(err), verbose, cliTest);
    }

    public ExecSupport(PrintWriter out, PrintWriter err, boolean verbose, boolean cliTest) {
        this.out = out;
        this.err = err;
        this.verbose = verbose;
        this.cliTest = cliTest;
    }

    public int executeProcess(String[] args, File parentDir) {
        if (isVerbose()) {
            out.println(String.join(" ", args));
            out.println();
        }

        var holder = new Object() {
            int exitCode;
        };
        ProcessBuilder<Void> pb = ProcessBuilder.newBuilder(args[0])
                .arguments(List.of(args).subList(1, args.length))
                .directory(parentDir.toPath())
                .exitCodeChecker(ec -> {
                    holder.exitCode = ec;
                    return true;
                })
                .input().empty();
        if (isCliTest()) {
            // We have to capture IO differently in tests..
            pb.output().transferTo(out)
                    .error().transferTo(err)
                    .run();
        } else {
            pb.output().inherited()
                    .error().inherited()
                    .run();
        }
        return holder.exitCode;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isCliTest() {
        return cliTest;
    }
}
