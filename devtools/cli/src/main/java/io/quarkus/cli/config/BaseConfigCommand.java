package io.quarkus.cli.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.smallrye.config.ConfigValue;
import picocli.CommandLine;

public class BaseConfigCommand {
    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

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

    protected ConfigValue findKey(List<String> lines, String name) {
        ConfigValue configValue = ConfigValue.builder().withName(name).build();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(configValue.getName() + "=")) {
                return configValue.withValue(line.substring(name.length() + 1)).withLineNumber(i);
            }
        }
        return configValue;
    }
}
