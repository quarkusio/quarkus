package io.quarkus.cli.common;

import io.quarkus.quickcli.annotations.Option;

public class ListFormatOptions {

    @Option(names = { "--id" }, order = 4, description = "Display extension artifactId only. (default)")
    boolean id = false;

    @Option(names = { "--name" }, hidden = true, description = "Display extension artifactId only. (deprecated)")
    boolean name = false;

    @Option(names = { "--concise" }, order = 5, description = "Display extension artifactId and name.")
    boolean concise = false;

    @Option(names = {
            "--full" }, order = 6, description = "Display extension artifactId, name, version, platform origin, and other information.")
    boolean full = false;

    @Option(names = {
            "--origins" }, order = 7, description = "Display extension artifactId, name, version, and platform origins.")
    boolean origins = false;

    @Option(names = {
            "--support-scope" }, order = 7, description = "Display extension artifactId, name, version, and support scope in case it's associated with an extension.")
    boolean supportScope = false;

    /**
     * Check if any format has been specified on the command line.
     */
    public boolean isSpecified() {
        return id || name || concise || full || origins;
    }

    public String getFormatString() {
        String formatString = "concise";
        if (id)
            formatString = "id";
        if (concise)
            formatString = "concise";
        else if (full)
            formatString = "full";
        else if (origins)
            formatString = "origins";
        else if (supportScope)
            formatString = "support-scope";
        return formatString;
    }
}
