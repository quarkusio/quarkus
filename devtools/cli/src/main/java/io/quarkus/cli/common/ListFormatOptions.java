package io.quarkus.cli.common;

import picocli.CommandLine;

public class ListFormatOptions {
    @CommandLine.Option(names = { "--name" }, order = 4, description = "Display extension name only.")
    boolean name = false;

    @CommandLine.Option(names = { "--concise" }, order = 5, description = "Display extension name and description.")
    boolean concise = false;

    @CommandLine.Option(names = {
            "--full" }, order = 6, description = "Display concise format and version related columns.")
    boolean full = false;

    @CommandLine.Option(names = {
            "--origins" }, order = 7, description = "Display extensions including their platform origins.")
    boolean origins = false;

    /**
     * If a value was not specified via options (all false),
     * make origins true. Used with specific platform list.
     */
    public void useOriginsUnlessSpecified() {
        if (name || concise || full || origins) {
            return;
        }
        origins = true;
    }

    public String getFormatString() {
        String formatString = "name";
        if (concise)
            formatString = "concise";
        else if (full)
            formatString = "full";
        else if (origins)
            formatString = "origins";
        return formatString;
    }
}
