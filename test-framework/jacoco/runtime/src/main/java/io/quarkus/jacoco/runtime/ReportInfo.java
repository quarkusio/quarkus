package io.quarkus.jacoco.runtime;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ReportInfo {

    public String reportDir;
    public Path dataFile;
    public final List<String> savedData = new ArrayList<>();
    public Set<String> sourceDirectories;
    public Set<String> classFiles;
    public String artifactId;
    public Path errorFile;

    public void emitError(String msg) {
        emitError(msg, null);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void emitError(String msg, Throwable exception) {
        System.err.println(msg);
        if (exception != null) {
            exception.printStackTrace();
        }
        System.err.flush();

        try (OutputStream out = Files.newOutputStream(errorFile, CREATE, APPEND)) {
            PrintStream ps = new PrintStream(out);
            ps.println();
            ps.println(new Date() + ": " + msg);
            if (exception != null) {
                exception.printStackTrace(ps);
            }
        } catch (IOException ignore) {
            //ignore
        }
    }
}
