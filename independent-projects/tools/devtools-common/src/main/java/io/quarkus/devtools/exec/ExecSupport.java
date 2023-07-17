package io.quarkus.devtools.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    public int executeProcess(String[] args, File parentDir) throws IOException, InterruptedException {
        int exit = 0;
        if (isVerbose()) {
            out.println(String.join(" ", args));
            out.println();
        }

        if (isCliTest()) {
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
                        .forEach(out::println);
            });
            service.submit(() -> {
                new BufferedReader(new InputStreamReader(process.getErrorStream())).lines()
                        .forEach(err::println);
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
        return exit;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isCliTest() {
        return cliTest;
    }
}
