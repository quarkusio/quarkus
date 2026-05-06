package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.quickcli.annotations.Option;

public class PluginCatalogOptions {

    @Option(names = {
            "--user" }, defaultValue = "", paramLabel = "USER", order = 4, description = "Use the user catalog.")
    boolean user;

    @Option(names = {
            "--user-dir" }, paramLabel = "USER_DIR", order = 5, description = "Use the user catalog directory.")
    public Optional<Path> userDirectory = Optional.empty();

}
