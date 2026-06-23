package io.quarkus.cli.common;

import io.quarkus.quickcli.annotations.Option;

public class HelpOption {
    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
    public boolean help;
}
