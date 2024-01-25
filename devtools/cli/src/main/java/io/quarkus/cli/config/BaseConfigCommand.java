package io.quarkus.cli.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;

public class BaseConfigCommand {
    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    Path projectRoot;

    protected Path projectRoot() {
        if (projectRoot == null) {
            projectRoot = output.getTestDirectory();
            if (projectRoot == null) {
                projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            }
        }
        return projectRoot;
    }

    protected String encodeToString(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
