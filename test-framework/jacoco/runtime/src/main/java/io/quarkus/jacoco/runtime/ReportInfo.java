package io.quarkus.jacoco.runtime;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReportInfo {

    public final boolean preferSerializedData;
    public String reportDir;
    public Path dataFile;
    public final List<String> savedData = new ArrayList<>();
    public Set<String> sourceDirectories = new HashSet<>();
    public Set<String> classFiles = new HashSet<>();
    public String artifactId;
    public Path errorFile;

    public ReportInfo(boolean preferSerializedData) {
        this.preferSerializedData = preferSerializedData;
    }

    public void emitError(String msg) {
        emitError(msg, null);
    }

    public Set<String> getSourceDirectories() {
        if (preferSerializedData) {
            Path reportSourcesFilePath = dataFile.getParent().resolve("report-sources.txt");
            if (Files.isReadable(reportSourcesFilePath)) {
                Set<String> ret = new HashSet<>();
                try {
                    for (String line : Files.readAllLines(reportSourcesFilePath)) {
                        String source = line.strip();
                        if (!source.isEmpty()) {
                            ret.add(source);
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return ret;
            }
        }
        return sourceDirectories;
    }

    public Set<String> getClassFiles() {
        if (preferSerializedData) {
            Path reportClassesFilePath = dataFile.getParent().resolve("report-classes.txt");
            if (Files.isReadable(reportClassesFilePath)) {
                Set<String> ret = new HashSet<>();
                try {
                    for (String line : Files.readAllLines(reportClassesFilePath)) {
                        String classFile = line.strip();
                        if (!classFile.isEmpty()) {
                            ret.add(classFile);
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return ret;
            }
        }
        return classFiles;
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
