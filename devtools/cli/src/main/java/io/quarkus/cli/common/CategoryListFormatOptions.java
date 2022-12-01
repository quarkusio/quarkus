package io.quarkus.cli.common;

import picocli.CommandLine;

public class CategoryListFormatOptions {
    @CommandLine.Option(names = { "--id" }, order = 1, description = "Display categoryId column only. (default)")
    boolean id = false;

    @CommandLine.Option(names = { "--concise" }, order = 2, description = "Display categoryId and name columns.")
    boolean concise = false;

    @CommandLine.Option(names = { "--full" }, order = 3, description = "Display categoryId, name and description columns.")
    boolean full = false;

    public String getFormatString() {
        String format = "id";
        if (concise)
            format = "concise";
        if (full)
            format = "full";
        return format;
    }

    /**
     * Check if any format has been specified on the command line.
     */
    public boolean isSpecified() {
        return id || concise || full;
    }
}
