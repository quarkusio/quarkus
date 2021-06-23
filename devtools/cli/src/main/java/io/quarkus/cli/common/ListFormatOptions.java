package io.quarkus.cli.common;

import picocli.CommandLine;

public class ListFormatOptions {

    @CommandLine.Option(names = { "--id" }, order = 4, description = "Display extension artifactId only. (default)")
    boolean id = false;

    @CommandLine.Option(names = { "--name" }, hidden = true, description = "Display extension artifactId only. (deprecated)")
    boolean name = false;

    @CommandLine.Option(names = { "--concise" }, order = 5, description = "Display extension name and artifactId.")
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
        if (id || name || concise || full || origins) {
            return;
        }
        origins = true;
    }

    /**
     * Check if any format has been specified on the command line.
     */
    public boolean isSpecified() {
        return id || name || concise || full || origins;
    }

    public String getFormatString() {
        String formatString = "id";
        if (concise)
            formatString = "concise";
        else if (full)
            formatString = "full";
        else if (origins)
            formatString = "origins";
        return formatString;
    }
}
