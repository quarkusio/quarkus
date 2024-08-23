package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.Optional;

import picocli.CommandLine;

public class PluginCatalogOptions {

    @CommandLine.Option(names = {
            "--user" }, defaultValue = "", paramLabel = "USER", order = 4, description = "Use the user catalog.")
    boolean user;

    @CommandLine.Option(names = {
            "--user-dir" }, paramLabel = "USER_DIR", order = 5, description = "Use the user catalog directory.")
    Optional<Path> userDirectory = Optional.empty();

}
