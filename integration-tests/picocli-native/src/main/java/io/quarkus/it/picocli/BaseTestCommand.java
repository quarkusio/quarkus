package io.quarkus.it.picocli;

import java.io.File;
import java.util.List;

import picocli.CommandLine;

public class BaseTestCommand {

    @CommandLine.Option(names = { "-f", "--files" }, description = "Some files.")
    private List<File> files;

    public List<File> getFiles() {
        return files;
    }
}
